package com.isariand.recettes.network

import com.isariand.recettes.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object OpenAiClient {

    private val authInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            .build()
        chain.proceed(req)
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api: OpenAiApiService = retrofit.create(OpenAiApiService::class.java)


}
