package com.isariand.recettes.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.isariand.recettes.R
import com.isariand.recettes.ui.detail.RecipeDetailFragment // 👈 Import du Fragment de Détail
import com.isariand.recettes.viewmodel.MainViewModel

class RecipeListFragment : Fragment(R.layout.fragment_recipe_list) {

    // Partage le MainViewModel, qui détient le LiveData des recettes sauvegardées
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: RecipeAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recipe_recycler_view)
        emptyTextView = view.findViewById(R.id.empty_list_text)

        // 1. Initialisation de l'Adapter avec la fonction de navigation
        adapter = RecipeAdapter { recipeId ->
            // Appel de la fonction pour lancer le Fragment de Détail
            navigateToRecipeDetail(recipeId)
        }
        recyclerView.adapter = adapter

        // 2. Observation des données de Room via le MainViewModel
        viewModel.savedRecipes.observe(viewLifecycleOwner, Observer { recipes ->
            adapter.submitList(recipes)

            // Affichage conditionnel
            if (recipes.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyTextView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyTextView.visibility = View.GONE
            }
        })
    }

    /**
     * Gère la navigation vers l'écran de détail en passant l'ID de la recette.
     */
    private fun navigateToRecipeDetail(recipeId: Long) {
        // Création du Fragment de Détail
        val detailFragment = RecipeDetailFragment()

        // Création des arguments
        val args = Bundle().apply {
            putLong("recipeId", recipeId)
        }
        detailFragment.arguments = args

        // Lancement de la transaction de Fragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment) // R.id.fragment_container est dans activity_main.xml
            .addToBackStack(null) // Permet de revenir en arrière
            .commit()
    }
}