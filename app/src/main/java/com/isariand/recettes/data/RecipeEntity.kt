package com.isariand.recettes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 💡 L'entité de base de données. On utilise les informations que vous avez déjà.
@Entity(tableName = "recipes")
data class RecipeEntity(
    // Clé primaire auto-générée
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val customTitle: String,

    val videoTitle: String,

    val dateAdded: Long,
    val videoUrl: String,
    val noWatermarkUrl: String? = null // Optionnel
)