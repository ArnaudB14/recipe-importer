package com.isariand.recettes.repository

import android.util.Log
import com.google.gson.Gson
import com.isariand.recettes.data.GeminiRecipe
import com.isariand.recettes.data.GeminiRecipeRaw
import com.isariand.recettes.data.RecipeDao
import com.isariand.recettes.data.RecipeEntity
import com.isariand.recettes.data.VideoData
import com.isariand.recettes.network.TikwmApiService
import kotlinx.coroutines.flow.Flow
import android.graphics.Bitmap
import com.google.ai.client.generativeai.type.content
import com.isariand.recettes.data.GeminiFridgeRaw
import com.isariand.recettes.network.GroqApiService
import com.isariand.recettes.network.OpenAiInputMessage
import com.isariand.recettes.network.OpenAiResponseRequest
import com.isariand.recettes.network.OpenAiTextConfig
import com.isariand.recettes.network.OpenAiTextFormat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class VideoRepository(
    private val apiService: TikwmApiService,
    private val recipeDao: RecipeDao,
    private val groq: GroqApiService
) {


    private val gson = Gson()

    suspend fun fetchVideoDetails(videoLink: String): Result<VideoData> {
        return try {
            val response = apiService.getNoWatermarkVideo(videoLink)
            if (response.code == 0 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeTextAndGetRecipe(recipeText: String): Result<GeminiRecipe> {
        val tag = "GroqText"

        return try {
            val prompt = """
Tu es un assistant spécialisé en recettes de cuisine.

Voici le texte brut d'une recette issue de TikTok :

"$recipeText"

⚠️ RÈGLES STRICTES :
- Réponds UNIQUEMENT avec un JSON valide
- AUCUN texte hors JSON
- Si une info est absente, mets une chaîne vide ""
- Si il n'y a pas d'instructions, invente-les de façon plausible.
- Si c'est en anglais, traduis en français.
- Estime les calories et macros si possible.
- N'ajoute PAS les macros dans la description.
- Ingrédients et instructions doivent être des chaînes multi-lignes (1 item par ligne), sans tirets.

FORMAT JSON OBLIGATOIRE :
{
  "title": "",
  "description": "",
  "ingredients": "",
  "instructions": "",
  "cookingTime": "",
  "portions": "",
  "macros": { "kcal":"", "p":"", "g":"", "l":"" }
}
""".trimIndent()

            Log.d(tag, "Calling Groq recipe analysis")

            val body = """
{
  "model": "llama-3.1-8b-instant",
  "temperature": 0.2,
  "response_format": { "type": "json_object" },
  "messages": [
    { "role": "user", "content": ${gson.toJson(prompt)} }
  ]
}
""".trimIndent()

            val resp = groq.chatCompletions(jsonBody(body))
            val raw = resp.choices.firstOrNull()?.message?.content.orEmpty().trim()

            if (raw.isBlank()) return Result.failure(Exception("Réponse Groq vide"))

            val parsed = gson.fromJson(raw, GeminiRecipeRaw::class.java)

            val recipe = GeminiRecipe(
                title = parsed.title,
                description = parsed.description,
                ingredients = anyToMultilineString(parsed.ingredients),
                instructions = anyToMultilineString(parsed.instructions),
                cookingTime = parsed.cookingTime,
                macros = parsed.macros,
                portions = parsed.portions.trim(),
            )

            Result.success(recipe)

        } catch (e: Exception) {
            Log.e(tag, "Analyze text error: ${e.message}", e)
            Result.failure(e)
        }
    }


    private fun bitmapToJpegDataUrl(bitmap: Bitmap): String {
        val baos = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        val b64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }

    suspend fun analyzeFridgeImage(bitmap: Bitmap): Result<List<String>> {
        val tag = "GroqFridge"

        return try {
            val prompt = """
Tu analyses une photo d'un frigo ou d'un plan de travail et tu listes les ingrédients visibles.

RÈGLES STRICTES :
- Réponds UNIQUEMENT avec un JSON valide
- AUCUN texte hors JSON
- Liste max 25 éléments
- Noms courts en français (ex: "oeufs", "lait", "tomates", "poulet", "fromage")
- Si tu doutes, n'invente pas

FORMAT JSON OBLIGATOIRE :
{ "items": ["", "", ""] }
""".trimIndent()

            val dataUrl = bitmapToJpegDataUrl(bitmap)

            val body = """
{
  "model": "llama-3.2-11b-vision-preview",
  "temperature": 0.2,
  "messages": [
    {
      "role": "user",
      "content": [
        { "type": "text", "text": ${gson.toJson(prompt)} },
        { "type": "image_url", "image_url": { "url": ${gson.toJson(dataUrl)} } }
      ]
    }
  ]
}
""".trimIndent()

            val resp = groq.chatCompletions(jsonBody(body))
            val raw = resp.choices.firstOrNull()?.message?.content.orEmpty().trim()

            if (raw.isBlank()) return Result.failure(Exception("Réponse Groq vide"))

            val parsed = gson.fromJson(raw, GeminiFridgeRaw::class.java)

            val items = parsed.items
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
                .take(25)

            Result.success(items)

        } catch (e: Exception) {
            Log.e(tag, "analyzeFridgeImage error: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun cleanMacroValue(s: String?): String {
        if (s.isNullOrBlank()) return ""
        // garde chiffres + . + ,
        val num = s.trim()
            .replace(",", ".")
            .replace(Regex("""[^0-9.]"""), "")
            .trim()
        // évite "": si rien de récupéré
        return num
    }


    suspend fun saveRecipe(
        videoUrl: String,
        videoData: VideoData,
        geminiRecipe: GeminiRecipe
    ) {
        if (recipeDao.countByVideoUrl(videoUrl) > 0) {
            return // déjà importée => pas d'appel Gemini
        }

        val protein = cleanMacroValue(geminiRecipe.macros?.p)
        val carbs = cleanMacroValue(geminiRecipe.macros?.g)
        val fat = cleanMacroValue(geminiRecipe.macros?.l)
        val kcal = cleanMacroValue(geminiRecipe.macros?.kcal)


        val macrosText = listOfNotNull(
            kcal.takeIf { it.isNotBlank() }?.let { "${it} kcal" },
            protein.takeIf { it.isNotBlank() }?.let { "P ${it}g" },
            carbs.takeIf { it.isNotBlank() }?.let { "G ${it}g" },
            fat.takeIf { it.isNotBlank() }?.let { "L ${it}g" }
        ).joinToString(" • ")

        val mp4Url = when {
            !videoData.noWatermarkUrl.isNullOrBlank() -> videoData.noWatermarkUrl
            !videoData.playUrl.isNullOrBlank() -> videoData.playUrl
            !videoData.watermarkUrl.isNullOrBlank() -> videoData.watermarkUrl
            else -> null
        }

        val recipeEntity = RecipeEntity(
            id = 0,
            customTitle = geminiRecipe.title,
            recipeTitle = geminiRecipe.title,
            videoTitle = videoData.title ?: "Titre non spécifié",
            description = geminiRecipe.description,
            ingredients = geminiRecipe.ingredients,
            instructions = geminiRecipe.instructions,
            cookingTime = geminiRecipe.cookingTime,
            kcal = kcal,
            protein = protein,
            carbs = carbs,
            fat = fat,
            macros = macrosText,
            portions = geminiRecipe.portions.trim(),
            dateAdded = System.currentTimeMillis(),
            videoUrl = videoUrl,
            noWatermarkUrl = mp4Url,
        )
        recipeDao.insert(recipeEntity)
    }

    fun getSavedRecipes(): Flow<List<RecipeEntity>> {
        return recipeDao.getAllRecipes()
    }

    suspend fun getRecipe(recipeId: Long): RecipeEntity? {
        return recipeDao.getRecipeById(recipeId)
    }

    suspend fun updateRecipeTitle(recipeId: Long, newTitle: String) {
        recipeDao.updateCustomTitle(recipeId, newTitle)
    }

    suspend fun deleteRecipe(recipeId: Long) {
        recipeDao.deleteById(recipeId)
    }

    suspend fun updateIngredients(recipeId: Long, value: String) {
        recipeDao.updateIngredients(recipeId, value)
    }

    suspend fun updateInstructions(recipeId: Long, value: String) {
        recipeDao.updateInstructions(recipeId, value)
    }

    suspend fun updateDescription(recipeId: Long, value: String) {
        recipeDao.updateDescription(recipeId, value)
    }

    suspend fun saveTags(recipeId: Long, tags: String) {
        recipeDao.updateTags(recipeId, tags)
    }

    suspend fun getAllTags(): List<String> {
        return recipeDao.getAllTagsStrings()
            .flatMap { it.split(",") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    suspend fun toggleFavorite(id: Long) {
        recipeDao.toggleFavorite(id)
    }

    fun observeRecipeById(id: Long) = recipeDao.observeRecipeById(id)

    private fun anyToMultilineString(value: Any?): String {
        return when (value) {
            null -> ""
            is String -> value.trim()
            is List<*> -> value
                .filterIsInstance<String>()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .trim()
            else -> value.toString().trim()
        }
    }

    data class RecipeMatch(
        val recipe: RecipeEntity,
        val score: Double,
        val matchedCount: Int,
        val totalCount: Int
    )

    private fun normalizeIngredient(s: String): String {
        return s.lowercase()
            .replace(Regex("""\([^)]*\)"""), " ") // enlève (facultatif)
            .replace(Regex("""\d+[.,]?\d*\s*(g|kg|ml|l|c\.|c|cs|cc|sachet|pincée|tbsp|tsp)?"""), " ")
            .replace(Regex("""[^\p{L}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    private fun parseRecipeIngredients(raw: String): Set<String> {
        return raw.split("\n")
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }
            .map { normalizeIngredient(it) }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun ingredientMatches(recipeIng: String, fridgeSet: Set<String>): Boolean {
        // exact
        if (recipeIng in fridgeSet) return true
        // contient / inclus (oignon vs oignon jaune)
        return fridgeSet.any { f -> f.contains(recipeIng) || recipeIng.contains(f) }
    }

    suspend fun findRecipesByFridgeIngredients(
        fridgeIngredients: List<String>,
        threshold: Double = 0.5
    ): List<RecipeMatch> {

        val allRecipes = recipeDao.getAllRecipesOnce()

        val fridgeSet = fridgeIngredients
            .map { normalizeIngredient(it) }
            .filter { it.isNotBlank() }
            .toSet()

        if (fridgeSet.isEmpty()) return emptyList()

        return allRecipes.mapNotNull { r ->
            val recipeSet = parseRecipeIngredients(r.ingredients)
            if (recipeSet.isEmpty()) return@mapNotNull null

            val matchedCount = recipeSet.count { ing -> ingredientMatches(ing, fridgeSet) }
            val score = matchedCount.toDouble() / recipeSet.size.toDouble()

            if (score >= threshold) {
                RecipeMatch(
                    recipe = r,
                    score = score,
                    matchedCount = matchedCount,
                    totalCount = recipeSet.size
                )
            } else null
        }.sortedByDescending { it.score }
    }

    suspend fun getAllRecipesOnce(): List<RecipeEntity> {
        return recipeDao.getAllRecipesOnce()
    }

    private fun jsonBody(s: String) =
        s.toRequestBody("application/json; charset=utf-8".toMediaType())


}
