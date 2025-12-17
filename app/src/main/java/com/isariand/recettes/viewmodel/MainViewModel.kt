package com.isariand.recettes.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import com.isariand.recettes.data.VideoData
import com.isariand.recettes.repository.VideoRepository
import com.isariand.recettes.data.RecipeEntity


class MainViewModel(private val repository: VideoRepository) : ViewModel() {
    private val _videoData = MutableLiveData<VideoData?>()
    private val _savedRecipes = repository.getSavedRecipes().asLiveData(viewModelScope.coroutineContext)
    val savedRecipes: LiveData<List<RecipeEntity>> = _savedRecipes
    val videoData: LiveData<VideoData?> = _videoData
    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage
    private val searchQuery = MutableLiveData<String>("")
    private val _selectedTags = androidx.lifecycle.MutableLiveData<Set<String>>(emptySet())
    val selectedTags: androidx.lifecycle.LiveData<Set<String>> = _selectedTags
    private val _fridgeMatches = MutableLiveData<List<VideoRepository.RecipeMatch>>(emptyList())
    val fridgeMatches: LiveData<List<VideoRepository.RecipeMatch>> = _fridgeMatches

    sealed class ImportState {
        object Idle : ImportState()
        data class Loading(val message: String = "Import en cours…") : ImportState()
        data class Success(val message: String = "Recette importée !") : ImportState()
        data class Error(val message: String = "Erreur d’import") : ImportState()
    }

    private val _importState = MutableLiveData<ImportState>(ImportState.Idle)
    val importState: LiveData<ImportState> = _importState

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }
    fun loadVideoDetails(link: String) {
        viewModelScope.launch {
            _errorMessage.value = null

            val result = repository.fetchVideoDetails(link)

            result.onSuccess { data ->
                _videoData.value = data
            }.onFailure { exception ->
                _videoData.value = null
                _errorMessage.value = exception.message ?: "Erreur inconnue."
            }
        }
    }

    fun importFromTiktok(sharedText: String) {
        viewModelScope.launch {
            // évite les doubles imports si on reçoit 2 intents
            if (_importState.value is ImportState.Loading) return@launch

            _importState.value = ImportState.Loading("Récupération de la vidéo…")

            val fetch = repository.fetchVideoDetails(sharedText)
            fetch.onSuccess { data ->
                _videoData.value = data

                val text = data.title
                if (text.isNullOrBlank()) {
                    _importState.value = ImportState.Error("Aucun texte de recette trouvé.")
                    return@launch
                }

                _importState.value = ImportState.Loading("Analyse de la recette…")

                try {
                    val analysis = repository.analyzeTextAndGetRecipe(text)
                    analysis.onSuccess { geminiRecipe ->
                        repository.saveRecipe(sharedText, data, geminiRecipe)
                        _importState.value = ImportState.Success("Recette importée !")
                    }.onFailure {
                        _importState.value = ImportState.Error("Analyse impossible (quota API)")
                    }
                } catch (e: retrofit2.HttpException) {
                    _importState.value = ImportState.Error("Quota API OpenAI non actif")
                }


            }.onFailure { e ->
                _importState.value = ImportState.Error(e.message ?: "Erreur TikWM")
            }
        }
    }

    fun deleteRecipe(recipeId: Long) {
        viewModelScope.launch {
            repository.deleteRecipe(recipeId)
        }
    }

    fun toggleTag(tag: String) {
        val current = _selectedTags.value.orEmpty().toMutableSet()
        val key = tag.trim()

        if (current.any { it.equals(key, ignoreCase = true) }) {
            current.removeIf { it.equals(key, ignoreCase = true) }
        } else {
            current.add(key)
        }
        _selectedTags.value = current
    }

    fun clearTags() {
        _selectedTags.value = emptySet()
    }

    fun toggleFavorite(recipeId: Long) {
        viewModelScope.launch {
            repository.toggleFavorite(recipeId)
        }
    }

    val filteredRecipes = androidx.lifecycle.MediatorLiveData<List<com.isariand.recettes.data.RecipeEntity>>().apply {

        fun applyFilter() {
            val all = savedRecipes.value.orEmpty()
            val q = searchQuery.value.orEmpty().trim().lowercase()
            val tags = selectedTags.value.orEmpty().map { it.lowercase() }.toSet()

            val res = all.filter { r ->
                val title = (r.customTitle ?: "").lowercase()
                val desc = (r.description ?: "").lowercase()
                val ing = (r.ingredients ?: "").lowercase()
                val instr = (r.instructions ?: "").lowercase()
                val tagsStr = (r.tags ?: "")

                val recipeTags = tagsStr.split(",")
                    .map { it.trim().lowercase() }
                    .filter { it.isNotBlank() }
                    .toSet()

                val matchText =
                    q.isBlank() || title.contains(q) || desc.contains(q) || ing.contains(q) || instr.contains(q) || tagsStr.lowercase().contains(q)

                val matchTags =
                    tags.isEmpty() || recipeTags.any { it in tags } // OR logique

                matchText && matchTags
            }

            value = res
        }

        addSource(savedRecipes) { applyFilter() }
        addSource(searchQuery) { applyFilter() }
        addSource(selectedTags) { applyFilter() }
    }

    private val _showFavoritesOnly = MutableLiveData(false)
    val showFavoritesOnly: LiveData<Boolean> = _showFavoritesOnly

    fun toggleFavoritesOnly() {
        _showFavoritesOnly.value = !(_showFavoritesOnly.value ?: false)
    }

    val visibleRecipes = MediatorLiveData<List<RecipeEntity>>().apply {
        fun apply() {
            val base = filteredRecipes.value.orEmpty()
            val favOnly = _showFavoritesOnly.value ?: false
            value = if (favOnly) base.filter { it.isFavorite } else base
        }

        addSource(filteredRecipes) { apply() }
        addSource(_showFavoritesOnly) { apply() }
    }
}

