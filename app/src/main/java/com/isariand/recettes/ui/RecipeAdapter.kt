package com.isariand.recettes.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.isariand.recettes.R
import com.isariand.recettes.data.RecipeEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecipeAdapter(
    // La fonction de rappel pour gérer le clic sur un élément de la liste.
    // Elle prend l'ID de la recette (Long) que nous allons utiliser pour la navigation.
    private val onRecipeClicked: (Long) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private var recipes: List<RecipeEntity> = emptyList()

    fun submitList(newRecipes: List<RecipeEntity>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_card, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.bind(recipe)
        // L'action au clic renvoie l'ID de la recette
        holder.itemView.setOnClickListener { onRecipeClicked(recipe.id) }
    }

    override fun getItemCount(): Int = recipes.size

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.recipe_title)
        private val date: TextView = itemView.findViewById(R.id.recipe_date)
        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        fun bind(recipe: RecipeEntity) {
            title.text = recipe.customTitle
            date.text = "Ajoutée: ${dateFormatter.format(Date(recipe.dateAdded))}"
        }
    }
}