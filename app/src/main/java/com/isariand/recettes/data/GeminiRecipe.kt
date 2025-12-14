package com.isariand.recettes.data // VÉRIFIEZ CE PACKAGE

data class GeminiRecipe(
    val title: String,
    val description: String,
    val ingredients: String,
    val instructions: String,
    val cookingTime: String?
)