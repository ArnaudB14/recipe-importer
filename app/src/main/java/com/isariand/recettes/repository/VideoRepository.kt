// Fichier: repository/VideoRepository.kt (corrigé)

package com.isariand.recettes.repository

import com.isariand.recettes.data.RecipeDao
import com.isariand.recettes.data.RecipeEntity
import com.isariand.recettes.data.VideoData
import com.isariand.recettes.network.TikwmApiService
import kotlinx.coroutines.flow.Flow
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.Part
import com.google.ai.client.generativeai.type.FileDataPart
import com.google.gson.Gson
import com.isariand.recettes.data.GeminiRecipe
import com.google.ai.client.generativeai.type.TextPart
import android.util.Log

class VideoRepository(
    private val apiService: TikwmApiService,
    private val recipeDao: RecipeDao,
    geminiApiKey: String
) {

    suspend fun testGeminiApi(): String {
        return try {
            val testResponse = geminiModel.generateContent("Dis-moi bonjour.")
            "Test réussi: ${testResponse.text}"
        } catch (e: Exception) {
            "Test échoué: ${e.message}"
        }
    }

    suspend fun fetchVideoDetails(videoLink: String): Result<VideoData> {
        return try {
            val response = apiService.getNoWatermarkVideo(videoLink)

            if (response.code == 0 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception("Erreur API : ${response.msg}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val geminiModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = geminiApiKey
    )

    private val gson = Gson()

    private fun logLong(tag: String, message: String, max: Int = 3500) {
        if (message.length <= max) {
            Log.d(tag, message)
            return
        }
        var i = 0
        while (i < message.length) {
            val end = (i + max).coerceAtMost(message.length)
            Log.d(tag, message.substring(i, end))
            i = end
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

FORMAT JSON OBLIGATOIRE :
{
  "title": "",
  "description": "",
  "ingredients": "",
  "instructions": "",
  "cookingTime": "",
  "macros": { "kcal":"", "p":"", "g":"", "l":"" }
}

- kcal ex "450"
- p/g/l en grammes ex "32"
- si inconnu => ""


""".trimIndent()

            Log.d(tag, "Sending TEXT to Gemini")
            Log.d(tag, recipeText)

            val response = geminiModel.generateContent(prompt)

            val raw = response.text
            Log.d(tag, "RAW RESPONSE:\n$raw")

            if (raw.isNullOrBlank()) {
                return Result.failure(Exception("Réponse Gemini vide"))
            }

            // Nettoyage sécurité (au cas où)
            val cleanedJson = raw
                .replace("```json", "")
                .replace("```", "")
                .trim()

            Log.d(tag, "CLEANED JSON:\n$cleanedJson")

            val recipe = gson.fromJson(cleanedJson, GeminiRecipe::class.java)

            Result.success(recipe)

        } catch (e: Exception) {
            Log.e(tag, "Analyze text error: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveRecipe(
        videoUrl: String,
        videoData: VideoData,        // Données brutes TikWM
        geminiRecipe: GeminiRecipe   // Données structurées Gemini
    ) {
        val recipeEntity = RecipeEntity(
            // Utilise le titre extrait par Gemini comme titre par défaut
            customTitle = geminiRecipe.title,

            // Champs structurés par Gemini
            recipeTitle = geminiRecipe.title,
            description = geminiRecipe.description,
            ingredients = geminiRecipe.ingredients,
            instructions = geminiRecipe.instructions,
            cookingTime = geminiRecipe.cookingTime,
            macros = geminiRecipe.macros,
            dateAdded = System.currentTimeMillis(),
            videoUrl = videoUrl,
            noWatermarkUrl = videoData.noWatermarkUrl,
            id = 0,
            videoTitle = videoData.title ?: "Titre non spécifié",
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

    fun searchRecipes(query: String): Flow<List<RecipeEntity>> {
        return recipeDao.searchRecipes(query)
    }

    suspend fun updateTags(recipeId: Long, tags: String) {
        recipeDao.updateTags(recipeId, tags)
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

    suspend fun setFavorite(recipeId: Long, isFav: Boolean) {
        recipeDao.setFavorite(recipeId, isFav)
    }

    suspend fun toggleFavorite(id: Long) {
        recipeDao.toggleFavorite(id)
    }

    fun observeRecipeById(id: Long) = recipeDao.observeRecipeById(id)

}
