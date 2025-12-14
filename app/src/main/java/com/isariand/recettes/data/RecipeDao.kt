package com.isariand.recettes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    // Pour insérer une nouvelle recette. S'il y a un conflit, remplace l'ancienne entrée.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: RecipeEntity)
    // Pour obtenir toutes les recettes et les observer en temps réel (Flow)
    @Query("SELECT * FROM recipes ORDER BY dateAdded DESC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    // Pour obtenir les détails d'une recette par son ID
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    suspend fun getRecipeById(recipeId: Long): RecipeEntity?

    @Query("UPDATE recipes SET customTitle = :newTitle WHERE id = :recipeId")
    suspend fun updateCustomTitle(recipeId: Long, newTitle: String)

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteById(recipeId: Long)

}