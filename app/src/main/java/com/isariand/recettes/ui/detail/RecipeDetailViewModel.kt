package com.isariand.recettes.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isariand.recettes.data.RecipeEntity // Utilisation de l'entité Room
import com.isariand.recettes.repository.VideoRepository // Votre Repository
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val repository: VideoRepository,
    private val recipeId: Long
) : ViewModel() {

    private val _recipe = MutableLiveData<RecipeEntity?>()
    val recipe: LiveData<RecipeEntity> = repository.observeRecipeById(recipeId)

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _allTags = MutableLiveData<List<String>>(emptyList())
    val allTags: LiveData<List<String>> = _allTags

    init {
        loadRecipeDetail()
    }

    private fun loadRecipeDetail() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val recipeDetail = repository.getRecipe(recipeId)

                _recipe.value = recipeDetail
            } catch (e: Exception) {
                _recipe.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveCustomTitle(newTitle: String) {
        if (newTitle.isBlank()) return

        viewModelScope.launch {
            repository.updateRecipeTitle(recipeId, newTitle)
        }
    }

    fun saveIngredients(text: String) {
        viewModelScope.launch {
            repository.updateIngredients(recipeId, text)
        }
    }

    fun saveInstructions(text: String) {
        viewModelScope.launch {
            repository.updateInstructions(recipeId, text)
        }
    }

    fun saveDescription(text: String) {
        viewModelScope.launch {
            repository.updateDescription(recipeId, text)
        }
    }

    fun saveTags(tags: String) {
        viewModelScope.launch {
            repository.saveTags(recipeId, tags)
        }
    }

    fun loadAllTags() {
        viewModelScope.launch {
            _allTags.value = repository.getAllTags()
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            repository.toggleFavorite(recipeId)
        }
    }

}
