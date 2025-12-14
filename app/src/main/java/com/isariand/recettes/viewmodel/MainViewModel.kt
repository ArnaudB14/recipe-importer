package com.isariand.recettes.viewmodel

import android.util.Log
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

// Fichier: viewmodel/MainViewModel.kt (Mise à jour de saveLastFetchedVideo)

    fun saveLastFetchedVideo(originalLink: String) {
        viewModelScope.launch {
            val tikwmData = videoData.value

            if (tikwmData == null) {
                _errorMessage.postValue("Aucune donnée vidéo à sauvegarder.")
                return@launch
            }

            val text = tikwmData.title
            if (text.isNullOrBlank()) {
                _errorMessage.postValue("Aucun texte de recette disponible.")
                return@launch
            }

            _errorMessage.postValue("Analyse de la recette en cours...")

            val analysisResult = repository.analyzeTextAndGetRecipe(text)

            analysisResult.onSuccess { geminiRecipe ->
                // ✅ suspend, donc on reste dans la coroutine (on est toujours dans launch)
                viewModelScope.launch {
                    repository.saveRecipe(originalLink, tikwmData, geminiRecipe)
                    _errorMessage.postValue("Recette analysée et sauvegardée ✅")
                }
            }.onFailure { e ->
                _errorMessage.postValue("Erreur analyse recette : ${e.message}")
            }
        }
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            repository.deleteRecipe(recipeId)
        }
    }


}