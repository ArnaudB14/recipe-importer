package com.isariand.recettes.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private fun colorForTag(tag: String): Int {
        val key = tag.trim().lowercase()
        val h = kotlin.math.abs(key.hashCode())

        val hue = (h % 360).toFloat()

        val sat = 0.30f + ((h / 360) % 10) / 100f    // 0.18 .. 0.27
        val value = 0.95f + ((h / 3600) % 4) / 100f  // 0.94 .. 0.97

        return Color.HSVToColor(floatArrayOf(hue, sat, value))
    }


    private fun darken(color: Int, amount: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * (1f - amount)).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
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
            val finalColor = if (isSelected) darken(color, 0.12f) else color
            DrawableCompat.setTint(bg, finalColor)

            val chip = TextView(holder.itemView.context).apply {
                text = tag
                textSize = 13f
                setTextColor(0xFF111827.toInt())
                background = bg
                setPadding(12, 6, 12, 6)
                typeface = ResourcesCompat.getFont(context, R.font.architects_daughter_regular)
                setOnClickListener { onTagClicked(tag) }
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = 10
                    bottomMargin = 8
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
