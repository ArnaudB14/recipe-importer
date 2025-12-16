package com.isariand.recettes.data // VÉRIFIEZ CE PACKAGE
data class GeminiMacros(
    val kcal: String = "",
    val p: String = "",
    val g: String = "",
    val l: String = ""
)
data class GeminiRecipe(
    val title: String,
    val description: String,
    val ingredients: String,
    val instructions: String,
    val cookingTime: String?,
    val macros: GeminiMacros
)