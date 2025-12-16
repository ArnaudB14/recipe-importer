package com.isariand.recettes.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipe: RecipeEntity)

    @Query("SELECT * FROM recipes ORDER BY dateAdded DESC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    suspend fun getRecipeById(recipeId: Long): RecipeEntity?

    @Query("UPDATE recipes SET customTitle = :newTitle WHERE id = :recipeId")
    suspend fun updateCustomTitle(recipeId: Long, newTitle: String)

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteById(recipeId: Long)

    @Query("""
    UPDATE recipes 
    SET ingredients = :ingredients
    WHERE id = :recipeId
""")
    suspend fun updateIngredients(recipeId: Long, ingredients: String)

    @Query("""
    UPDATE recipes 
    SET instructions = :instructions
    WHERE id = :recipeId
""")
    suspend fun updateInstructions(recipeId: Long, instructions: String)

    @Query("""
    UPDATE recipes 
    SET description = :description
    WHERE id = :recipeId
""")
    suspend fun updateDescription(recipeId: Long, description: String)

    @Query("""
    SELECT * FROM recipes
    WHERE
        customTitle LIKE '%' || :query || '%'
        OR recipeTitle LIKE '%' || :query || '%'
        OR ingredients LIKE '%' || :query || '%'
        OR tags LIKE '%' || :query || '%'
    ORDER BY dateAdded DESC
""")
    fun searchRecipes(query: String): Flow<List<RecipeEntity>>

    @Query("UPDATE recipes SET tags = :tags WHERE id = :recipeId")
    suspend fun updateTags(recipeId: Long, tags: String)

    @Query("SELECT tags FROM recipes WHERE tags IS NOT NULL AND tags != ''")
    suspend fun getAllTagsStrings(): List<String>

    @Query("UPDATE recipes SET isFavorite = :isFav WHERE id = :recipeId")
    suspend fun setFavorite(recipeId: Long, isFav: Boolean)

    @Query("SELECT isFavorite FROM recipes WHERE id = :recipeId LIMIT 1")
    suspend fun isFavorite(recipeId: Long): Boolean

    @Query("UPDATE recipes SET isFavorite = CASE WHEN isFavorite = 1 THEN 0 ELSE 1 END WHERE id = :id")
    suspend fun toggleFavorite(id: Long)

    @Query("SELECT * FROM recipes WHERE id = :id LIMIT 1")
    fun observeRecipeById(id: Long): LiveData<RecipeEntity>

    @Query("SELECT COUNT(*) FROM recipes WHERE videoUrl = :url LIMIT 1")
    suspend fun countByVideoUrl(url: String): Int

}