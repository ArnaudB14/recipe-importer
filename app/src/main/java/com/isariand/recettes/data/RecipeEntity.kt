package com.isariand.recettes.data

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val customTitle: String,
    val recipeTitle: String,
    val description: String,
    val ingredients: String,
    val instructions: String,
    val cookingTime: String?,
    val videoTitle: String,
    val dateAdded: Long,
    val videoUrl: String,
    val noWatermarkUrl: String? = null,
    val tags: String = "",
    val isFavorite: Boolean = false,
    val portions: String = "",
    val macros: String = "",
    val kcal: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = ""
)