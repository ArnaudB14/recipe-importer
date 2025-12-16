package com.isariand.recettes.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.isariand.recettes.R
import com.isariand.recettes.data.RecipeEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.flexbox.FlexboxLayout
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.drawable.DrawableCompat
import android.widget.ImageView
class RecipeAdapter(
    private val onRecipeClicked: (Long) -> Unit,
    private val onFavoriteClicked: (Long) -> Unit,
    private val onTagClicked: (String) -> Unit
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private var recipes: List<RecipeEntity> = emptyList()
    private var selectedTags: Set<String> = emptySet()

    private val TAG_COLORS = listOf(
        0xFFFFE08A.toInt(),
        0xFFFFB3BA.toInt(),
        0xFFBAFFC9.toInt(),
        0xFFBAE1FF.toInt(),
        0xFFD7BAFF.toInt(),
        0xFFFFD6A5.toInt(),
        0xFFBFFCC6.toInt(),
        0xFFFFF5BA.toInt(),
        0xFFC7CEEA.toInt(),
        0xFFFFC6FF.toInt()
    )

    private fun colorForTag(tag: String): Int {
        val normalized = tag.trim().lowercase()
        val idx = kotlin.math.abs(normalized.hashCode()) % TAG_COLORS.size
        return TAG_COLORS[idx]
    }

    private fun isDark(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b)
        return luminance < 140
    }

    fun submitList(newRecipes: List<RecipeEntity>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }


    fun setSelectedTags(tags: Set<String>) {
        selectedTags = tags.map { it.lowercase() }.toSet()
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

        holder.itemView.setOnClickListener {
            onRecipeClicked(recipe.id)
        }

        holder.favIcon.setOnClickListener {
            onFavoriteClicked(recipe.id)
        }

        holder.tagContainer.removeAllViews()

        val tags = recipe.tags
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        tags.forEach { tag ->
            val isSelected = selectedTags.contains(tag.lowercase())
            val bg = holder.itemView.context.getDrawable(R.drawable.sketch_tag)!!.mutate()
            val color = colorForTag(tag)
            DrawableCompat.setTint(bg, color)
            val chip = TextView(holder.itemView.context).apply {
                text = tag
                textSize = 13f
                setTextColor(if (isDark(color)) Color.WHITE else 0xFF111827.toInt())
                background = bg
                setPadding(12, 6, 12, 6)
                typeface = ResourcesCompat.getFont(context, R.font.architects_daughter_regular)
                setOnClickListener { onTagClicked(tag) }
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = 10 // espace horizontal
                    bottomMargin = 8 // espace vertical si ça wrap sur 2 lignes
                }
            }

            holder.tagContainer.addView(chip)
        }

    }

    override fun getItemCount(): Int = recipes.size

    class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.recipe_title)
        private val date: TextView = itemView.findViewById(R.id.recipe_date)
        val favIcon: ImageView = itemView.findViewById(R.id.fav_icon)
        private val metaBadgesContainer: ViewGroup = itemView.findViewById(R.id.metaBadgesContainer)
        val tagContainer: FlexboxLayout = itemView.findViewById(R.id.tagContainer)

        private val dateFormatter =
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        fun bind(recipe: RecipeEntity) {
            title.text = recipe.customTitle
            date.text = "Ajoutée le ${dateFormatter.format(Date(recipe.dateAdded))}"
            val icon = if (recipe.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            favIcon.setImageResource(icon)
            metaBadgesContainer.removeAllViews()

            if (recipe.kcal.isNotBlank()) metaBadgesContainer.addView(makeMetaBadge(itemView.context, "${recipe.kcal} kcal"))
            if (recipe.protein.isNotBlank()) metaBadgesContainer.addView(makeMetaBadge(itemView.context, "P ${recipe.protein}g"))
            if (recipe.carbs.isNotBlank()) metaBadgesContainer.addView(makeMetaBadge(itemView.context, "G ${recipe.carbs}g"))
            if (recipe.fat.isNotBlank()) metaBadgesContainer.addView(makeMetaBadge(itemView.context, "L ${recipe.fat}g"))

        }

        private fun makeMetaBadge(ctx: Context, text: String): TextView {
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

    }
}
