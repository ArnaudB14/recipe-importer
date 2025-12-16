package com.isariand.recettes.repository

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.gson.Gson
import com.isariand.recettes.data.GeminiRecipe
import com.isariand.recettes.data.GeminiRecipeRaw
import com.isariand.recettes.data.RecipeDao
import com.isariand.recettes.data.RecipeEntity
import com.isariand.recettes.data.VideoData
import com.isariand.recettes.network.TikwmApiService
import kotlinx.coroutines.flow.Flow

class VideoRepository(
    private val apiService: TikwmApiService,
    private val recipeDao: RecipeDao,
    geminiApiKey: String
) {

    private val geminiModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = geminiApiKey
    )

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
        val tag = "GeminiText"

        return try {
            val prompt = """
Tu es un assistant spécialisé en recettes de cuisine.

Voici le texte brut d'une recette issue de TikTok :

"$recipeText"

Ta tâche :
- Structurer cette recette proprement
- Déduire les ingrédients et quantités
- Déduire les étapes de préparation
- Estimer les macros si possible

⚠️ RÈGLES STRICTES :
- Réponds UNIQUEMENT avec un JSON valide
- AUCUN texte hors JSON
- Si une info est absente, mets une chaîne vide ""
- Si il n'y a pas d'instructions, invente les en te basant sur le titre de la recette et les ingrédients.
- Si c'est en anglais, traduis le en français.
- Estime les calories ainsi que les macros si possible.
- N'ajoute PAS les macros dans la description.
- Ne numérote pas ls instructions, il y en a déjà d'ajouté automatiquement mais mets les quand même en liste.
- N'ajoute pas de tiret dans la liste des ingrédients, il y en déjà d'ajouté automatiquement mais mets les quand même en liste.

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

- kcal ex "450"
- p/g/l en grammes ex "32"
- si inconnu => ""

- portions ex "2" ou "4"
- Essaye d'estimer, et si tu n'y arrives pas => ""

""".trimIndent()

            Log.d(tag, "Sending TEXT to Gemini")
            Log.d(tag, recipeText)

            val response = retryGemini {
                geminiModel.generateContent(prompt)
            }

            val raw = response.text
            Log.d(tag, "RAW RESPONSE:\n$raw")

            if (raw.isNullOrBlank()) {
                return Result.failure(Exception("Réponse Gemini vide"))
            }

            val cleanedJson = raw
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.d(tag, "CLEANED JSON:\n$cleanedJson")

            val rawRecipe = gson.fromJson(cleanedJson, GeminiRecipeRaw::class.java)

            val recipe = GeminiRecipe(
                title = rawRecipe.title,
                description = rawRecipe.description,
                ingredients = anyToMultilineString(rawRecipe.ingredients),
                instructions = anyToMultilineString(rawRecipe.instructions),
                cookingTime = rawRecipe.cookingTime,
                macros = rawRecipe.macros,
                portions = rawRecipe.portions.trim(),
            )

            Result.success(recipe)


        } catch (e: Exception) {
            Log.e(tag, "Analyze text error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveRecipe(
        videoUrl: String,
        videoData: VideoData,
        geminiRecipe: GeminiRecipe
    ) {
        if (recipeDao.countByVideoUrl(videoUrl) > 0) {
            return // déjà importée => pas d'appel Gemini
        }

        val kcal = geminiRecipe.macros?.kcal.orEmpty()
        val protein = geminiRecipe.macros?.p.orEmpty()
        val carbs = geminiRecipe.macros?.g.orEmpty()
        val fat = geminiRecipe.macros?.l.orEmpty()

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

    private suspend fun <T> retryGemini(
        times: Int = 3,
        initialDelayMs: Long = 500,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var delayMs = initialDelayMs
        var last: Exception? = null

        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                last = e
                val msg = e.message.orEmpty()
                val overloaded = msg.contains("overloaded", ignoreCase = true) ||
                        msg.contains("\"code\": 503") ||
                        msg.contains("UNAVAILABLE", ignoreCase = true)

                if (!overloaded || attempt == times - 1) throw e

                kotlinx.coroutines.delay(delayMs)
                delayMs = (delayMs * factor).toLong()
            }
        }
        throw last ?: RuntimeException("Gemini retry failed")
    }

}
