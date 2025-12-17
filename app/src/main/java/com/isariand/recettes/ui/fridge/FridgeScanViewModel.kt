package com.isariand.recettes.ui.fridge

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isariand.recettes.data.RecipeDao
import com.isariand.recettes.data.RecipeEntity
import com.isariand.recettes.repository.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.Normalizer

class FridgeScanViewModel(
    private val repository: VideoRepository,
    private val recipeDao: RecipeDao
) : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _detectedItems = MutableLiveData<List<String>>(emptyList())
    val detectedItems: LiveData<List<String>> = _detectedItems

    private val _matchingRecipesCount = MutableLiveData(0)
    val matchingRecipesCount: LiveData<Int> = _matchingRecipesCount

    private val _toast = MutableLiveData("")
    val toast: LiveData<String> = _toast

    data class MatchUi(
        val id: Long,
        val title: String,
        val scorePercent: Int
    )

    private val _matches = MutableLiveData<List<RecipeEntity>>(emptyList())
    val matches: LiveData<List<RecipeEntity>> = _matches


    fun analyze(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)

            val res = repository.analyzeFridgeImage(bitmap)
            if (res.isSuccess) {
                val items = res.getOrNull().orEmpty()
                _detectedItems.postValue(items)

                // ✅ auto-find
                findMatchingRecipes(items)

            } else {
                _toast.postValue("Analyse frigo impossible : ${res.exceptionOrNull()?.message}")
            }

            _loading.postValue(false)
        }
    }

    fun findMatchingRecipes(items: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            if (items.isEmpty()) {
                _matchingRecipesCount.postValue(0)
                _matches.postValue(emptyList())
                return@launch
            }

            val matches = repository.findRecipesByFridgeIngredients(items, threshold = 0.35)
            _matchingRecipesCount.postValue(matches.size)
            _matches.postValue(matches.map { it.recipe })
        }
    }




    private fun normalize(s: String): String {
        val lower = s.trim().lowercase()
        val noAccents = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
        return noAccents
            .replace(Regex("[^a-z0-9\\s-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractIngredients(text: String): Set<String> {
        return text.split("\n")
            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
            .filter { it.isNotBlank() }
            .map { normalize(it) }
            .toSet()
    }

    private fun recipeMatchScore(fridge: Set<String>, ingredientsText: String): Double {
        val recipeIngs = extractIngredients(ingredientsText)
        if (recipeIngs.isEmpty()) return 0.0

        val matched = recipeIngs.count { ing ->
            fridge.any { f -> ing.contains(f) || f.contains(ing) }
        }

        return matched.toDouble() / recipeIngs.size.toDouble()
    }

}
