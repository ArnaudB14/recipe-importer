package com.isariand.recettes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// 💡 L'entité de base de données. On utilise les informations que vous avez déjà.
@Entity(tableName = "recipes")
data class RecipeEntity(
    // Clé primaire auto-générée
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Titre personnalisé par l'utilisateur
    val customTitle: String,

    // 🛑 Nouveaux champs extraits par Gemini 🛑
    val recipeTitle: String,         // Titre extrait (pour l'affichage par défaut si customTitle est vide)
    val description: String,         // Description détaillée (avant les étapes)
    val ingredients: String,         // Liste formatée (ou JSON si vous le souhaitez, ici String)
    val instructions: String,        // Étapes formatées (ici String)
    val cookingTime: String?,        // Temps de cuisson (peut être null)
    val videoTitle: String,
    // Anciens champs
    val dateAdded: Long,
    val videoUrl: String,
    val noWatermarkUrl: String? = null
)