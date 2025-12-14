package com.isariand.recettes.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VideoResponse(
    val code: Int,
    val msg: String,
    val data: VideoData?
)

@JsonClass(generateAdapter = true)
data class VideoData(
    // Titre renvoyé par TikWM
    @Json(name = "title")
    val title: String? = null,

    // URL de lecture (souvent présent)
    @Json(name = "play")
    val playUrl: String? = null,

    // URL HD / sans watermark (souvent "hdplay" chez TikWM)
    @Json(name = "hdplay")
    val noWatermarkUrl: String? = null,

    // URL avec watermark (si tu veux la garder)
    @Json(name = "wmplay")
    val watermarkUrl: String? = null
)
