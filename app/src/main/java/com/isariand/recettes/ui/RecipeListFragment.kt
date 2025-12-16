package com.isariand.recettes.ui

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.isariand.recettes.R
import com.isariand.recettes.ui.detail.RecipeDetailFragment
import com.isariand.recettes.viewmodel.MainViewModel
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.core.widget.addTextChangedListener
import com.isariand.recettes.databinding.FragmentRecipeListBinding


class RecipeListFragment : Fragment(R.layout.fragment_recipe_list) {
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: RecipeAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private var _binding: FragmentRecipeListBinding? = null
    private val binding get() = _binding!!

    private var currentRecipes: List<com.isariand.recettes.data.RecipeEntity> = emptyList()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recipe_recycler_view)
        emptyTextView = view.findViewById(R.id.empty_list_text)
        _binding = FragmentRecipeListBinding.bind(view)

        adapter = RecipeAdapter(
            onRecipeClicked = { recipeId -> navigateToRecipeDetail(recipeId) },
            onFavoriteClicked = { recipeId -> viewModel.toggleFavorite(recipeId) },
            onTagClicked = { tag -> viewModel.toggleTag(tag) }
        )
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
                    viewModel.deleteRecipe(recipe.id)
                } else {
                    adapter.notifyItemChanged(position)
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)

        viewModel.visibleRecipes.observe(viewLifecycleOwner) { recipes ->
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

        fun renderActiveTagFilters(tags: Set<String>) {
            binding.activeTagFilters.removeAllViews()

            tags.forEach { tag ->
                val chip = TextView(requireContext()).apply {
                    text = "✕ $tag"
                    textSize = 13f
                    setTextColor(requireContext().getColor(R.color.sk_text))
                    background = requireContext().getDrawable(R.drawable.sketch_tag_selected)
                    setPadding(18, 10, 18, 10)
                    typeface = ResourcesCompat.getFont(context, R.font.architects_daughter_regular)

                    val lp = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, 12, 12)
                    layoutParams = lp

                    setOnClickListener { viewModel.toggleTag(tag) }
                }
                binding.activeTagFilters.addView(chip)
            }
        }

        viewModel.selectedTags.observe(viewLifecycleOwner) { tags ->
            adapter.setSelectedTags(tags)
            renderActiveTagFilters(tags)
        }

        binding.searchInput.addTextChangedListener { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }

        binding.favFilterChip.setOnClickListener {
            viewModel.toggleFavoritesOnly()
        }

        viewModel.showFavoritesOnly.observe(viewLifecycleOwner) { enabled ->
            binding.favFilterChip.alpha = if (enabled) 1f else 0.6f
        }

        binding.scanFridgeButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, com.isariand.recettes.ui.fridge.FridgeScanFragment())
                .addToBackStack(null)
                .commit()
        }

    }

    private fun navigateToRecipeDetail(recipeId: Long) {
        val detailFragment = RecipeDetailFragment()
        val args = Bundle().apply {
            putLong("recipeId", recipeId)
        }
        detailFragment.arguments = args
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, detailFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}