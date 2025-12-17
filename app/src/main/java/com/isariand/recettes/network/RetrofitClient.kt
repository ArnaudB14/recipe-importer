package com.isariand.recettes.network

import com.isariand.recettes.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object RetrofitClient {

    private const val BASE_URL = "https://www.tikwm.com"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }

    val apiService: TikwmApiService by lazy {
        retrofit.create(TikwmApiService::class.java)
    }

    val openAiClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(req)
        }
        .build()

    val openAiRetrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(openAiClient)
        .addConverterFactory(MoshiConverterFactory.create()) // ou GsonConverterFactory
        .build()

    val openAiApi: OpenAiApiService = openAiRetrofit.create(OpenAiApiService::class.java)

}