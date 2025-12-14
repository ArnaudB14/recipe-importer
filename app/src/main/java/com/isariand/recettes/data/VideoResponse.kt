package com.isariand.recettes.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class VideoResponse(
    // code doit être 0 pour que la requête soit un succès
    val code: Int,
    val msg: String,
    // La donnée principale qui contient les détails de la vidéo
    val data: VideoData?
)

@JsonClass(generateAdapter = true)
data class VideoData(
    // C'est ce champ qui contient la description/recette
    val title: String?,

    // Le lien de téléchargement (utile si vous voulez télécharger plus tard)
    @Json(name = "play")
    val videoUrl: String?,
)