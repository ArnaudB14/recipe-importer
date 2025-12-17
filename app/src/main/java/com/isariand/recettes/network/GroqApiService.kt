package com.isariand.recettes.network

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface GroqApiService {
    @POST("chat/completions")
    suspend fun chatCompletions(@Body body: RequestBody): GroqChatResponse
}

data class GroqChatResponse(
    val choices: List<GroqChoice> = emptyList()
)

data class GroqChoice(
    val message: GroqMessage
)

data class GroqMessage(
    val content: String? = null
)
