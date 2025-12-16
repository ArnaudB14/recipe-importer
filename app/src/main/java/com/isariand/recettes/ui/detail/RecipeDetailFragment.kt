package com.isariand.recettes.ui.detail

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.flexbox.FlexboxLayout
import com.isariand.recettes.R
import com.isariand.recettes.data.AppDatabase
import com.isariand.recettes.databinding.FragmentRecipeDetailBinding
import com.isariand.recettes.network.RetrofitClient
import com.isariand.recettes.repository.VideoRepository
import java.lang.IllegalArgumentException

class RecipeDetailFragment : Fragment(R.layout.fragment_recipe_detail) {

    private var _binding: FragmentRecipeDetailBinding? = null
    private val binding get() = _binding!!

    private val recipeId: Long by lazy { arguments?.getLong("recipeId") ?: 0L }

    private val viewModel: RecipeDetailViewModel by viewModels {
        val GEMINI_API_KEY_SECRET = "AIzaSyCU2v3XBpYQK1yPfSZ8zJLf9kTtbfSyIYg"
        val apiService = RetrofitClient.apiService
        val recipeDao = AppDatabase.getDatabase(requireContext()).recipeDao()
        val repository = VideoRepository(apiService, recipeDao, geminiApiKey = GEMINI_API_KEY_SECRET)
        RecipeDetailViewModelFactory(repository, recipeId)
    }

    private fun Int.dp(): Int =
        (this * requireContext().resources.displayMetrics.density).toInt()

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

                layoutParams = FlexboxLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 12, 12) }

                setOnClickListener {
                    val newTags = tags.filter { it != tag }.joinToString(", ")
                    binding.tagsInput.setText(newTags)
                    renderTags(newTags)
                    viewModel.saveTags(newTags)
                }
            }
            container.addView(chip)
        }
    }

    private fun renderIngredientsSketchy(raw: String) {
        val container = binding.ingredientsDisplay
        container.removeAllViews()

        raw.split("\n")
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                container.addView(makeIngredientRowRoundBullet(line))
            }
    }

    private fun makeIngredientRowRoundBullet(text: String): View {
        val ctx = requireContext()

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.TOP
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12.dp()) }
        }

        val bullet = View(ctx).apply {
            val size = 10.dp()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(6.dp(), 8.dp(), 14.dp(), 0)
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(ctx, R.color.sk_text))
            }
            alpha = 0.9f
        }

        val tv = TextView(ctx).apply {
            this.text = text
            textSize = 15f
            setTextColor(ContextCompat.getColor(ctx, R.color.sk_text))
            typeface = ResourcesCompat.getFont(ctx, R.font.architects_daughter_regular)
            setLineSpacing(6.dp().toFloat(), 1.03f)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        row.addView(bullet)
        row.addView(tv)
        return row
    }

    private fun renderInstructionsSketchy(raw: String) {
        val container = binding.instructionsDisplay
        container.removeAllViews()

        raw.split("\n")
            .map { it.trim().replaceFirst(Regex("^\\d+[.)]?\\s*"), "").trim() }
            .filter { it.isNotBlank() }
            .forEachIndexed { index, step ->
                container.addView(makeInstructionRowAligned(index + 1, step))
            }
    }

    private fun makeInstructionRowAligned(number: Int, text: String): View {
        val ctx = requireContext()

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.TOP
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 18.dp()) }
        }

        val badgeSize = 34.dp()

        val badge = TextView(ctx).apply {
            this.text = number.toString()
            textSize = 14f
            typeface = ResourcesCompat.getFont(ctx, R.font.architects_daughter_regular)
            setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
            gravity = android.view.Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(badgeSize, badgeSize).apply {
                setMargins(0, 2.dp(), 14.dp(), 0)
            }

            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ContextCompat.getColor(ctx, R.color.sk_text))
            }
        }

        val tv = TextView(ctx).apply {
            this.text = text
            textSize = 15f
            setTextColor(ContextCompat.getColor(ctx, R.color.sk_text))
            typeface = ResourcesCompat.getFont(ctx, R.font.architects_daughter_regular)
            setLineSpacing(6.dp().toFloat(), 1.05f)
            layoutParams = LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        }

        row.addView(badge)
        row.addView(tv)
        return row
    }

    private fun renderTagSuggestions(query: String) {
        val all = viewModel.allTags.value.orEmpty()

        val current = binding.tagsInput.text.toString()
            .split(",")
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()

        val filtered = if (query.isBlank()) {
            all
        } else {
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

            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 12, 12) }

            setOnClickListener { onClick() }
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

        viewModel.recipe.observe(viewLifecycleOwner) { recipe ->
            recipe?.let {
                binding.recipeTitle.setText(it.customTitle)
                binding.ingredientsContent.setText(it.ingredients)
                renderIngredientsSketchy(it.ingredients)
                binding.ingredientsContent.visibility = View.GONE
                binding.ingredientsDisplay.visibility = View.VISIBLE
                binding.instructionsContent.setText(it.instructions)
                renderInstructionsSketchy(it.instructions)
                binding.instructionsContent.visibility = View.GONE
                binding.instructionsDisplay.visibility = View.VISIBLE
                binding.tagsInput.setText(it.tags)
                renderTags(it.tags)

                binding.metaBadgesContainer.removeAllViews()

                val macros = it.macros.trim()
                if (macros.isNotBlank()) {
                    binding.metaBadgesContainer.addView(makeMetaBadge(requireContext(), macros))
                }

                val portions = it.portions.trim()
                if (portions.isNotBlank()) {
                    binding.metaBadgesContainer.addView(
                        makeColoredMetaBadge(requireContext(), "$portions pers.", R.color.sk_portions)
                    )
                }

                val icon = if (it.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
                binding.favButton.setImageResource(icon)

                val tiktokUrl = it.videoUrl
                if (!tiktokUrl.isNullOrBlank()) {
                    binding.openTikTokButton.visibility = View.VISIBLE
                    binding.openTikTokButton.setOnClickListener {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tiktokUrl)))
                    }
                } else {
                    binding.openTikTokButton.visibility = View.GONE
                }

            } ?: run {
                binding.recipeTitle.setText("Recette introuvable")
                binding.openTikTokButton.visibility = View.GONE
            }
        }

        binding.favButton.setOnClickListener { viewModel.toggleFavorite() }
        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.shareButton.setOnClickListener {
            shareRecipe()
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
                renderIngredientsSketchy(text)

                binding.ingredientsContent.visibility = View.GONE
                binding.ingredientsDisplay.visibility = View.VISIBLE
            }
        }
        binding.ingredientsDisplay.setOnClickListener {
            binding.ingredientsDisplay.visibility = View.GONE
            binding.ingredientsContent.visibility = View.VISIBLE
            binding.ingredientsContent.requestFocus()
        }

        binding.instructionsContent.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.instructionsContent.text.toString()
                viewModel.saveInstructions(text)
                renderInstructionsSketchy(text)

                binding.instructionsContent.visibility = View.GONE
                binding.instructionsDisplay.visibility = View.VISIBLE
            }
        }
        binding.instructionsDisplay.setOnClickListener {
            binding.instructionsDisplay.visibility = View.GONE
            binding.instructionsContent.visibility = View.VISIBLE
            binding.instructionsContent.requestFocus()
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
            renderTagSuggestions(text?.toString().orEmpty().trim())
        }
        viewModel.allTags.observe(viewLifecycleOwner) {
            renderTagSuggestions(binding.tagsInput.text.toString().trim())
        }
    }

    private fun buildShareText(): String {
        val r = viewModel.recipe.value ?: return ""

        val title = (if (r.customTitle.trim().isNotEmpty()) r.customTitle else r.recipeTitle).trim()
        val portions = r.portions.trim()
        val macros = r.macros.trim()

        val ingredients = r.ingredients
            .split("\n")
            .map { it.trim().removePrefix("-").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")

        val instructions = r.instructions
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapIndexed { i, line -> "${i + 1}. $line" }
            .joinToString("\n")

        val link = r.videoUrl.trim()

        return buildString {
            append(title).append("\n\n")

            if (portions.isNotEmpty()) append("👥 ").append(portions).append(" pers.\n")
            if (macros.isNotEmpty()) append("📊 ").append(macros).append("\n")

            append("\n🧾 Ingrédients\n")
            append(ingredients)

            append("\n\n👩‍🍳 Préparation\n")
            append(instructions)

            if (link.isNotEmpty()) {
                append("\n\n🔗 TikTok : ").append(link)
            }
        }
    }

    private fun shareRecipe() {
        val text = buildShareText()
        if (text.isBlank()) return

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }

        val ctx = context ?: return
        if (sendIntent.resolveActivity(ctx.packageManager) != null) {
            startActivity(Intent.createChooser(sendIntent, "Partager la recette"))
        } else {
            Toast.makeText(ctx, "Aucune application de partage trouvée.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private fun makeMetaBadge(ctx: android.content.Context, text: String): TextView {
    return TextView(ctx).apply {
        this.text = text
        textSize = 13f
        setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
        setPadding(22, 10, 22, 10)
        typeface = ResourcesCompat.getFont(ctx, R.font.architects_daughter_regular)

        background = GradientDrawable().apply {
            cornerRadius = 999f
            setColor(ContextCompat.getColor(ctx, R.color.sk_text))
        }

        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 12, 12) }
    }
}

private fun makeColoredMetaBadge(
    ctx: android.content.Context,
    text: String,
    bgColorRes: Int
): TextView {
    return TextView(ctx).apply {
        this.text = text
        textSize = 13f
        setTextColor(ContextCompat.getColor(ctx, android.R.color.white))
        setPadding(22, 10, 22, 10)
        typeface = ResourcesCompat.getFont(ctx, R.font.architects_daughter_regular)

        background = GradientDrawable().apply {
            cornerRadius = 999f
            setColor(ContextCompat.getColor(ctx, bgColorRes))
        }

        layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 12, 12) }
    }
}

class RecipeDetailViewModelFactory(
    private val repository: VideoRepository,
    private val recipeId: Long
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeDetailViewModel(repository, recipeId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
