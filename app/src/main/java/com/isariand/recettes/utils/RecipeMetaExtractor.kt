package com.isariand.recettes.utils

data class RecipeMeta(
    val time: String = "",
    val portions: String = "",
    val macros: String = ""
)

object RecipeMetaExtractor {

    fun extract(text: String): RecipeMeta {
        val src = text.lowercase()

        val time = extractTime(src)
        val portions = extractPortions(src)
        val macros = extractMacros(src)

        return RecipeMeta(time = time, portions = portions, macros = macros)
    }

    private fun extractTime(src: String): String {
        // ex: "10 min", "cuisson 20 min", "1 h", "1h30"
        val patterns = listOf(
            Regex("""(préparation|prep|cuisson|temps)\s*[:\-]?\s*(\d{1,3})\s*(min|mn|minutes)"""),
            Regex("""(préparation|prep|cuisson|temps)\s*[:\-]?\s*(\d{1,2})\s*(h|heure|heures)"""),
            Regex("""\b(\d{1,3})\s*(min|mn|minutes)\b"""),
            Regex("""\b(\d{1,2})\s*(h|heure|heures)\b"""),
            Regex("""\b(\d{1,2})h(\d{1,2})\b""") // 1h30
        )

        for (p in patterns) {
            val m = p.find(src) ?: continue
            return when {
                m.groupValues.size >= 4 && m.groupValues[1].isNotBlank() ->
                    "${m.groupValues[1].trim()} ${m.groupValues[2]} ${m.groupValues[3]}".trim()
                m.groupValues.size >= 3 ->
                    "${m.groupValues[1]} ${m.groupValues[2]}".trim()
                else -> m.value.trim()
            }
        }
        return ""
    }

    private fun extractPortions(src: String): String {
        // ex: "pour 2", "4 personnes", "2 portions"
        val patterns = listOf(
            Regex("""\b(pour|servir)\s*(\d{1,2})\b"""),
            Regex("""\b(\d{1,2})\s*(personnes|pers|parts|portions)\b""")
        )
        for (p in patterns) {
            val m = p.find(src) ?: continue
            return when {
                m.groupValues.size >= 3 && m.groupValues[1] in listOf("pour", "servir") ->
                    "${m.groupValues[2]} personnes"
                m.groupValues.size >= 3 ->
                    "${m.groupValues[1]} ${m.groupValues[2]}"
                else -> m.value
            }
        }
        return ""
    }

    private fun extractMacros(src: String): String {
        // ex: "420 kcal", "protéines 35g", "P:35 C:20 F:10"
        val kcal = Regex("""\b(\d{2,4})\s*(kcal|calories)\b""").find(src)?.groupValues?.get(1)

        fun findGram(label: String): String? {
            return Regex("""\b$label\s*[:\-]?\s*(\d{1,3})\s*g\b""").find(src)?.groupValues?.get(1)
        }

        val p = findGram("protéines|proteines|prot|p")
        val c = findGram("glucides|glucide|carbs|c")
        val f = findGram("lipides|lipide|fat|f")

        val parts = mutableListOf<String>()
        if (!p.isNullOrBlank()) parts.add("P ${p}g")
        if (!c.isNullOrBlank()) parts.add("G ${c}g")
        if (!f.isNullOrBlank()) parts.add("L ${f}g")
        if (!kcal.isNullOrBlank()) parts.add("${kcal} kcal")

        return parts.joinToString(" • ")
    }
}
