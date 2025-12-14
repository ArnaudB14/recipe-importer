// Fichier: repository/VideoRepository.kt (Modification)

package com.isariand.recettes.repository

import com.isariand.recettes.data.RecipeDao
import com.isariand.recettes.data.RecipeEntity
import com.isariand.recettes.data.VideoData
import com.isariand.recettes.network.TikwmApiService
import kotlinx.coroutines.flow.Flow

class VideoRepository(
    private val apiService: TikwmApiService,
    private val recipeDao: RecipeDao
) {
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

// Fichier: repository/VideoRepository.kt

    // ...
    suspend fun saveRecipe(videoUrl: String, videoData: VideoData) {
        // ...
        val recipeEntity = RecipeEntity(
            // id sera généré automatiquement

            // 🛑 INITIALISATION DU TITRE PERSONNALISÉ 🛑
            customTitle = videoData.title ?: "Recette sans titre",

            // Utilisation du nouveau nom de champ
            videoTitle = videoData.title ?: "Description non disponible",

            dateAdded = System.currentTimeMillis(),
            videoUrl = videoUrl,
        )
        recipeDao.insert(recipeEntity)
    }

    fun getSavedRecipes(): Flow<List<RecipeEntity>> {
        return recipeDao.getAllRecipes()
    }

    suspend fun getRecipe(recipeId: Long): RecipeEntity? {
        // Appelle la méthode du DAO pour récupérer l'entité
        return recipeDao.getRecipeById(recipeId)
    }

    suspend fun updateRecipeTitle(recipeId: Long, newTitle: String) {
        recipeDao.updateCustomTitle(recipeId, newTitle)
    }
}