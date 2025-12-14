package com.isariand.recettes.utils

object ApiKeyProvider {
    var geminiApiKey: String? = null
        private set

    fun initialize(key: String) {
        if (geminiApiKey == null) {
            geminiApiKey = key
        }
    }
}