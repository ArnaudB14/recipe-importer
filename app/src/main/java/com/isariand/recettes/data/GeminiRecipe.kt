package com.isariand.recettes.data

data class GeminiMacros(
    val kcal: String = "",
    val p: String = "",
    val g: String = "",
    val l: String = ""
)

data class GeminiRecipeRaw(
    val title: String = "",
    val description: String = "",
    val ingredients: Any? = null,
    val instructions: Any? = null,
    val cookingTime: String? = null,
    val portions: String = "",
    val macros: GeminiMacros = GeminiMacros()
)

data class GeminiRecipe(
    val title: String = "",
    val description: String = "",
    val ingredients: String = "",
    val instructions: String = "",
    val portions: String = "",
    val cookingTime: String? = null,
    val macros: GeminiMacros = GeminiMacros()
)
