package com.isariand.recettes.data

// Objet macros inchangé
data class GeminiMacros(
    val kcal: String = "",
    val p: String = "",
    val g: String = "",
    val l: String = ""
)

// ✅ Modèle "raw" : accepte String OU Array pour ingredients/instructions
data class GeminiRecipeRaw(
    val title: String = "",
    val description: String = "",
    val ingredients: Any? = null,
    val instructions: Any? = null,
    val cookingTime: String? = null,
    val portions: String = "",
    val macros: GeminiMacros = GeminiMacros()
)

// ✅ Modèle normalisé : celui que tu utilises dans l'app (String partout)
data class GeminiRecipe(
    val title: String = "",
    val description: String = "",
    val ingredients: String = "",
    val instructions: String = "",
    val portions: String = "",
    val cookingTime: String? = null,
    val macros: GeminiMacros = GeminiMacros()
)
