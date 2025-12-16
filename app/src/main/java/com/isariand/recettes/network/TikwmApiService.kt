package com.isariand.recettes.network

import com.isariand.recettes.data.VideoResponse
import retrofit2.http.GET // Importez la classe GET
import retrofit2.http.Query

interface TikwmApiService {

    @GET("/api/")
    suspend fun getNoWatermarkVideo(
        @Query("url") videoLink: String
    ): VideoResponse
}