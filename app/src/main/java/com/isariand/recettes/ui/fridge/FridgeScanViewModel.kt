package com.isariand.recettes.ui.fridge

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isariand.recettes.data.RecipeDao
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

    fun analyze(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.postValue(true)

            val res = repository.analyzeFridgeImage(bitmap)
            if (res.isSuccess) {
                _detectedItems.postValue(res.getOrNull().orEmpty())
            } else {
                _toast.postValue("Analyse frigo impossible : ${res.exceptionOrNull()?.message}")
            }

            _loading.postValue(false)
        }
    }

    fun findMatchingRecipes() {
        viewModelScope.launch(Dispatchers.IO) {
            val items = _detectedItems.value.orEmpty()
            if (items.isEmpty()) {
                _toast.postValue("Aucun ingrédient détecté.")
                return@launch
            }

            val fridgeSet = items.map { normalize(it) }.toSet()

            // on récupère toutes les recettes une fois (c’est OK si tu n’en as pas 10 000)
            val all = recipeDao.getAllRecipesOnce()
            val matches = all.count { recipe ->
                recipeMatchesAll(fridgeSet, recipe.ingredients)
            }

            _matchingRecipesCount.postValue(matches)
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

    private fun recipeMatchesAll(fridge: Set<String>, ingredientsText: String): Boolean {
        val recipeIngs = extractIngredients(ingredientsText)
        if (recipeIngs.isEmpty()) return false

        return recipeIngs.all { ing ->
            fridge.any { f -> ing.contains(f) || f.contains(ing) }
        }
    }
}
