package com.isariand.recettes.ui.detail

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.isariand.recettes.R
import com.isariand.recettes.data.RecipeEntity // 💡 Correction: Utiliser RecipeEntity
import com.isariand.recettes.databinding.FragmentRecipeDetailBinding // Assurez-vous d'avoir activé viewBinding
import com.isariand.recettes.repository.VideoRepository // 💡 Correction: Utiliser VideoRepository
import com.isariand.recettes.network.RetrofitClient // Pour l'injection manuelle
import com.isariand.recettes.data.AppDatabase // Pour l'injection manuelle
import com.squareup.picasso.Picasso
import java.lang.IllegalArgumentException
import android.widget.TextView
class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var customTitleTextView: TextView
    private lateinit var videoTitleTextView: TextView
    // 💡 CORRECTION 1 : L'ID est un Long (clé primaire de Room)
    private val recipeId: Long by lazy {
        arguments?.getLong("recipeId") ?: 0L
    }

    // 💡 CORRECTION 3 : Initialisation du ViewModel avec injection manuelle
    private val viewModel: RecipeDetailViewModel by viewModels {

        // 1. Créer les dépendances du Repository
        val apiService = RetrofitClient.apiService
        val recipeDao = AppDatabase.getDatabase(requireContext()).recipeDao()
        val repository = VideoRepository(apiService, recipeDao) // Votre Repository

        // 2. Utiliser la Factory et passer les dépendances
        RecipeDetailViewModelFactory(repository, recipeId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRecipeDetailBinding.bind(view)
        // Gérer l'erreur si l'ID n'a pas été passé
        if (recipeId == 0L) {
            Toast.makeText(context, "Erreur: ID de recette manquant.", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Observer les données du ViewModel
        viewModel.recipe.observe(viewLifecycleOwner) { recipe ->
            recipe?.let {
                // Afficher le titre dans l'EditText
                binding.recipeTitle.setText(it.customTitle)

                binding.videoDescription.text = "Description : ${it.videoTitle}"
                // ... (Picasso si vous l'avez gardé)
            } ?: run {
                binding.recipeTitle.setText("Recette introuvable")
            }
        }

        binding.recipeTitle.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // L'utilisateur vient de perdre le focus (a fini d'éditer)
                val newTitle = binding.recipeTitle.text.toString().trim()
                val currentRecipe = viewModel.recipe.value

                // Sauvegarder uniquement si le nouveau titre est différent de l'ancien
                if (currentRecipe != null && newTitle != currentRecipe.customTitle) {
                    if (newTitle.isNotBlank()) {
                        viewModel.saveCustomTitle(newTitle)
                    } else {
                        // Si le titre est vide, on peut le remettre au titre original TikTok ou laisser l'utilisateur
                        // gérer cela. Ici, on utilise la description comme titre par défaut.
                        viewModel.saveCustomTitle(currentRecipe.videoTitle)
                        Toast.makeText(context, "Titre personnalisé effacé, retour au titre vidéo.", Toast.LENGTH_SHORT).show()
                    }

                    // Mettre à jour visuellement pour confirmer la sauvegarde
                    Toast.makeText(context, "Titre mis à jour : $newTitle", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Observer l'état de chargement
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Ajoutez ici la logique pour un ProgressBar (si vous en avez un)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


// 💡 CORRECTION : La Factory adaptée aux types Long et VideoRepository
class RecipeDetailViewModelFactory(
    private val repository: VideoRepository, // 👈 Utiliser VideoRepository
    private val recipeId: Long // 👈 Utiliser Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeDetailViewModel(repository, recipeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}