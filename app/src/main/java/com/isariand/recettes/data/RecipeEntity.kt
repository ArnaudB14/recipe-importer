package com.isariand.recettes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val customTitle: String,

    @ColumnInfo(defaultValue = "''")
    val recipeTitle: String,

    @ColumnInfo(defaultValue = "''")
    val description: String,

    @ColumnInfo(defaultValue = "''")
    val ingredients: String,

    @ColumnInfo(defaultValue = "''")
    val instructions: String,

    val cookingTime: String?,

    val videoTitle: String,
    val dateAdded: Long,
    val videoUrl: String,
    val noWatermarkUrl: String? = null,

    @ColumnInfo(defaultValue = "''")
    val tags: String = "",

    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false,

    @ColumnInfo(defaultValue = "''")
    val portions: String = "",

    @ColumnInfo(defaultValue = "''")
    val macros: String = "",

    @ColumnInfo(defaultValue = "''")
    val kcal: String = "",

    @ColumnInfo(defaultValue = "''")
    val protein: String = "",

    @ColumnInfo(defaultValue = "''")
    val carbs: String = "",

    @ColumnInfo(defaultValue = "''")
    val fat: String = ""
)
