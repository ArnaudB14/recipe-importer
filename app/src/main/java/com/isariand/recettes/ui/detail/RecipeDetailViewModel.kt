package com.isariand.recettes.ui.detail

// Imports adaptés
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isariand.recettes.data.RecipeEntity // Utilisation de l'entité Room
import com.isariand.recettes.repository.VideoRepository // Votre Repository
import kotlinx.coroutines.launch

// ⚠️ L'ID est maintenant un Long
class RecipeDetailViewModel(
    private val repository: VideoRepository,
    private val recipeId: Long
) : ViewModel() {

    private val _recipe = MutableLiveData<RecipeEntity?>()
    val recipe: LiveData<RecipeEntity?> = _recipe

    private val _isLoading = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    // ... (pas d'erreur LiveData pour l'instant)

    init {
        loadRecipeDetail()
    }

    private fun loadRecipeDetail() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Appel au Repository mis à jour
                val recipeDetail = repository.getRecipe(recipeId)

                _recipe.value = recipeDetail
            } catch (e: Exception) {
                _recipe.value = null
                // Gérer les erreurs ici
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveCustomTitle(newTitle: String) {
        // Optionnel : validation simple
        if (newTitle.isBlank()) return

        viewModelScope.launch {
            repository.updateRecipeTitle(recipeId, newTitle)

            // Recharger la recette pour mettre à jour l'interface utilisateur
            // (Si votre LiveData 'recipe' est basé sur un Flow du DAO, il sera mis à jour automatiquement)
            // Si vous n'utilisez pas de Flow, vous devrez peut-être recharger manuellement la recette ici.
        }
    }
}
