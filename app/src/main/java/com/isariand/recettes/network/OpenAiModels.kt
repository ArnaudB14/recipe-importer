package com.isariand.recettes.network

data class OpenAiResponseRequest(
    val model: String,
    val input: List<OpenAiInputMessage>,
    val text: OpenAiTextConfig? = null
)

data class OpenAiInputMessage(
    val role: String,
    val content: String
)

data class OpenAiTextConfig(
    val format: OpenAiTextFormat
)

data class OpenAiTextFormat(
    val type: String
)

data class OpenAiResponse(
    val output: List<OpenAiOutputItem>?
)

data class OpenAiResponsesRaw(
    val output: List<OpenAiOutputItem>?
)

data class OpenAiOutputItem(
    val content: List<OpenAiOutputContent>?
)

data class OpenAiOutputContent(
    val type: String?,
    val text: String?
)
