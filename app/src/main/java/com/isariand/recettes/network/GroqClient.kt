package com.isariand.recettes.network

import com.isariand.recettes.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object GroqClient {

    private val authInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${BuildConfig.GROQ_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .build()
        chain.proceed(req)
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.groq.com/openai/v1/")
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: GroqApiService = retrofit.create(GroqApiService::class.java)
}
