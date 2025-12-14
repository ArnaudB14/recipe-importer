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
import androidx.recyclerview.widget.ItemTouchHelper


class RecipeListFragment : Fragment(R.layout.fragment_recipe_list) {

    // Partage le MainViewModel, qui détient le LiveData des recettes sauvegardées
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: RecipeAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView

    private var currentRecipes: List<com.isariand.recettes.data.RecipeEntity> = emptyList()


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

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val recipe = currentRecipes.getOrNull(position)
                if (recipe != null) {
                    // 👇 ici, le nom du champ dépend de ton Entity
                    viewModel.deleteRecipe(recipe.id)
                } else {
                    // si on ne retrouve pas l’item (rare), on redessine la liste
                    adapter.notifyItemChanged(position)
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)


        // 2. Observation des données de Room via le MainViewModel
        viewModel.savedRecipes.observe(viewLifecycleOwner) { recipes ->
            currentRecipes = recipes
            adapter.submitList(recipes)

            if (recipes.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyTextView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyTextView.visibility = View.GONE
            }
        }

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