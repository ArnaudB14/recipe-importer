package com.isariand.recettes.ui.detail

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.isariand.recettes.R
import com.isariand.recettes.databinding.FragmentRecipeDetailBinding
import com.isariand.recettes.repository.VideoRepository
import com.isariand.recettes.network.RetrofitClient
import com.isariand.recettes.data.AppDatabase
import java.lang.IllegalArgumentException
import android.widget.TextView
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.res.ResourcesCompat
import com.google.android.flexbox.FlexboxLayout
import androidx.core.widget.addTextChangedListener

class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var customTitleTextView: TextView
    private lateinit var videoTitleTextView: TextView
    private val recipeId: Long by lazy {
        arguments?.getLong("recipeId") ?: 0L
    }

    private val viewModel: RecipeDetailViewModel by viewModels {

        val GEMINI_API_KEY_SECRET = "AIzaSyCU2v3XBpYQK1yPfSZ8zJLf9kTtbfSyIYg"
        val apiService = RetrofitClient.apiService
        val recipeDao = AppDatabase.getDatabase(requireContext()).recipeDao()
        val repository = VideoRepository(apiService, recipeDao, geminiApiKey = GEMINI_API_KEY_SECRET) // Votre Repository
        RecipeDetailViewModelFactory(repository, recipeId)
    }

    private fun renderTags(tagsRaw: String) {
        val container = binding.tagsContainer
        container.removeAllViews()

        val tags = tagsRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        tags.forEach { tag ->
            val chip = TextView(requireContext()).apply {
                text = "$tag  ×"
                textSize = 14f
                setTextColor(requireContext().getColor(R.color.sk_text))
                background = requireContext().getDrawable(R.drawable.sketch_tag)
                typeface = ResourcesCompat.getFont(requireContext(), R.font.architects_daughter_regular)

                setPadding(18, 10, 18, 10)

                // marges entre chips
                layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 12, 12)
                }

                setOnClickListener {
                    val newTags = tags.filter { it != tag }.joinToString(", ")
                    // 1) update UI
                    binding.tagsInput.setText(newTags)
                    renderTags(newTags)
                    // 2) persist
                    viewModel.saveTags(newTags)
                }
            }

            container.addView(chip)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentRecipeDetailBinding.bind(view)
        if (recipeId == 0L) {
            Toast.makeText(context, "Erreur: ID de recette manquant.", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return
        }

        // Observer les données du ViewModel
        viewModel.recipe.observe(viewLifecycleOwner) { recipe ->
            recipe?.let {
                binding.recipeTitle.setText(it.customTitle)
                binding.videoDescription.setText(it.description)
                binding.ingredientsContent.setText(it.ingredients)
                binding.instructionsContent.setText(it.instructions)
                binding.tagsInput.setText(it.tags ?: "")
                binding.metaBadgesContainer.removeAllViews()

                val macros = it.macros.trim()
                if (macros.isNotBlank()) {
                    binding.metaBadgesContainer.addView(makeMetaBadge(macros))
                }

                val icon = if (it.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                binding.favButton.setImageResource(icon)

                renderTags(it.tags ?: "")

                val tiktokUrl = it.videoUrl
                if (!tiktokUrl.isNullOrBlank()) {
                    binding.openTikTokButton.visibility = View.VISIBLE
                    binding.openTikTokButton.setOnClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(tiktokUrl))
                        startActivity(intent)
                    }
                } else {
                    binding.openTikTokButton.visibility = View.GONE
                }

            } ?: run {
                binding.recipeTitle.setText("Recette introuvable")
                binding.openTikTokButton.visibility = View.GONE
            }
        }

        binding.favButton.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }


        binding.recipeTitle.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val newTitle = binding.recipeTitle.text.toString().trim()
                val currentRecipe = viewModel.recipe.value
                if (currentRecipe != null && newTitle != currentRecipe.customTitle) {
                    if (newTitle.isNotBlank()) {
                        viewModel.saveCustomTitle(newTitle)
                    } else {
                        viewModel.saveCustomTitle(currentRecipe.videoTitle)
                        Toast.makeText(context, "Titre personnalisé effacé, retour au titre vidéo.", Toast.LENGTH_SHORT).show()
                    }

                    Toast.makeText(context, "Titre mis à jour : $newTitle", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.ingredientsContent.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.ingredientsContent.text.toString()
                viewModel.saveIngredients(text)
            }
        }

        binding.instructionsContent.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.instructionsContent.text.toString()
                viewModel.saveInstructions(text)
            }
        }

        binding.videoDescription.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.videoDescription.text.toString()
                viewModel.saveDescription(text)
            }
        }

        binding.tagsInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val tagsText = binding.tagsInput.text.toString()
                renderTags(tagsText)
                viewModel.saveTags(tagsText)
            }
        }

        viewModel.loadAllTags()

        binding.tagsInput.addTextChangedListener { text ->
            val q = text?.toString().orEmpty().trim()
            renderTagSuggestions(q)
        }

        viewModel.allTags.observe(viewLifecycleOwner) {
            renderTagSuggestions(binding.tagsInput.text.toString().trim())
        }

    }

    private fun renderTagSuggestions(query: String) {
        val all = viewModel.allTags.value.orEmpty()

        // Tags déjà présents dans l'input
        val current = binding.tagsInput.text.toString()
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        val filtered = if (query.isBlank()) {
            all
        } else {
            // On suggère en fonction du "dernier morceau" tapé après la dernière virgule
            val lastToken = query.substringAfterLast(",").trim().lowercase()
            if (lastToken.isBlank()) all
            else all.filter { it.lowercase().contains(lastToken) }
        }
            .filter { it.lowercase() !in current }
            .take(12)

        binding.tagSuggestionsContainer.removeAllViews()
        filtered.forEach { tag ->
            binding.tagSuggestionsContainer.addView(makeTagChip(tag) {
                addTagToInput(tag)
                renderTagSuggestions(binding.tagsInput.text.toString())
            })
        }
    }

    private fun addTagToInput(tag: String) {
        val existing = binding.tagsInput.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toMutableList()

        if (existing.any { it.equals(tag, ignoreCase = true) }) return

        existing.add(tag)
        binding.tagsInput.setText(existing.joinToString(", "))
        binding.tagsInput.setSelection(binding.tagsInput.text.length)
    }

    private fun makeTagChip(tag: String, onClick: () -> Unit): TextView {
        return TextView(requireContext()).apply {
            text = tag
            textSize = 14f
            setTextColor(resources.getColor(R.color.sk_text, null))
            background = resources.getDrawable(R.drawable.sketch_tag, null)
            setPadding(18, 10, 18, 10)
            typeface = ResourcesCompat.getFont(context, R.font.architects_daughter_regular)

            // marge entre tags
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 12, 12)
            }

            setOnClickListener { onClick() }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun makeMetaBadge(view: View, text: String): TextView {
    val ctx = view.context
    return TextView(ctx).apply {
        this.text = text
        textSize = 13f
        setTextColor(ctx.getColor(android.R.color.white))
        setPadding(22, 10, 22, 10)
        typeface = ResourcesCompat.getFont(ctx, R.font.architects_daughter_regular)

        background = GradientDrawable().apply {
            cornerRadius = 999f
            setColor(ctx.getColor(R.color.sk_text))
        }

        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 12, 12) }
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