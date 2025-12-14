package com.isariand.recettes.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import com.isariand.recettes.data.VideoData
import com.isariand.recettes.repository.VideoRepository
import com.isariand.recettes.data.RecipeEntity
class MainViewModel(private val repository: VideoRepository) : ViewModel() {

    // LiveData qui contient les données de la vidéo (y compris la recette/description)
    private val _videoData = MutableLiveData<VideoData?>()
    private val _savedRecipes = repository.getSavedRecipes().asLiveData(viewModelScope.coroutineContext)
    val savedRecipes: LiveData<List<RecipeEntity>> = _savedRecipes
    val videoData: LiveData<VideoData?> = _videoData

    // LiveData pour les messages d'erreur à afficher
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun loadVideoDetails(link: String) {
        viewModelScope.launch {
            _errorMessage.value = null // Vider l'erreur

            val result = repository.fetchVideoDetails(link)

            result.onSuccess { data ->
                _videoData.value = data
            }.onFailure { exception ->
                _videoData.value = null
                _errorMessage.value = exception.message ?: "Erreur inconnue."
            }
        }
    }

// Fichier: viewmodel/MainViewModel.kt (CORRIGÉ)

    fun saveLastFetchedVideo(originalLink: String) {
        val data = videoData.value
        if (data != null) {
            viewModelScope.launch {
                try {
                    // 🛑 CORRECTION : Inverser les arguments 🛑
                    // Signature attendue : saveRecipe(videoUrl: String, videoData: VideoData)
                    repository.saveRecipe(originalLink, data) // data est VideoData, originalLink est String

                    // Note: La fonction saveRecipe ne retourne probablement pas l'ID de la DB
                    // (elle retourne Unit ou Long/void selon votre DAO).
                    // Si votre DAO retourne Unit, retirez le "val newId ="
                    // S'il retourne l'ID (Long), vous pouvez le laisser.

                    // Si saveRecipe retourne Unit:
                    // repository.saveRecipe(originalLink, data)
                    _errorMessage.postValue("Recette sauvegardée.")

                } catch (e: Exception) {
                    _errorMessage.postValue("Erreur de sauvegarde: ${e.message}")
                }
            }
        } else {
            _errorMessage.postValue("Aucune donnée vidéo à sauvegarder.")
        }
    }
}