package com.isariand.recettes.network

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAiApiService {

    @Headers(
        "Content-Type: application/json"
    )
    @POST("v1/responses")
    suspend fun createResponse(@Body body: OpenAiResponseRequest): OpenAiResponsesRaw

    @POST("v1/responses")
    suspend fun createResponseRaw(@Body body: RequestBody): OpenAiResponsesRaw

}