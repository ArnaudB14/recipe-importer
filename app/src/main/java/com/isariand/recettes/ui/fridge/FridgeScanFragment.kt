package com.isariand.recettes.ui.fridge

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.isariand.recettes.BuildConfig
import com.isariand.recettes.R
import com.isariand.recettes.data.AppDatabase
import com.isariand.recettes.databinding.FragmentFridgeScanBinding
import com.isariand.recettes.network.GroqClient
import com.isariand.recettes.network.RetrofitClient
import com.isariand.recettes.repository.VideoRepository
import com.isariand.recettes.ui.RecipeAdapter
import com.isariand.recettes.ui.detail.RecipeDetailFragment

class FridgeScanFragment : Fragment(R.layout.fragment_fridge_scan) {

    private var _binding: FragmentFridgeScanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FridgeScanViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.apiService
                val dao = AppDatabase.getDatabase(requireContext()).recipeDao()
                val repo = VideoRepository(api, dao, groq = GroqClient.api)
                @Suppress("UNCHECKED_CAST")
                return FridgeScanViewModel(repo, dao) as T
            }
        }
    }

    private val takePicturePreview =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
            if (bmp != null) {
                binding.photoPreview.setImageBitmap(bmp)
                viewModel.analyze(bmp)
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFridgeScanBinding.bind(view)

        binding.takePhotoButton.setOnClickListener {
            takePicturePreview.launch(null)
        }

        val adapter = RecipeAdapter(
            onRecipeClicked = { id ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, RecipeDetailFragment().apply {
                        arguments = Bundle().apply { putLong("recipeId", id) }
                    })
                    .addToBackStack(null)
                    .commit()
            },
            onFavoriteClicked = { id -> /* optionnel: ignore ou toggle */ },
            onTagClicked = { tag -> /* optionnel: ignore */ }
        )

        binding.matchesRecycler.adapter = adapter
        binding.matchesRecycler.layoutManager = LinearLayoutManager(requireContext())


        // Observers
        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            binding.progress.visibility = if (loading) View.VISIBLE else View.GONE
            binding.takePhotoButton.isEnabled = !loading
            binding.findRecipesButton.isEnabled = !loading
        }

        viewModel.detectedItems.observe(viewLifecycleOwner) { items ->
            binding.itemsContainer.text = items.joinToString(", ")
        }

        viewModel.matchingRecipesCount.observe(viewLifecycleOwner) { count ->
            binding.resultsInfo.text = "Recettes trouvées : $count"
        }

        viewModel.matches.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }


        viewModel.toast.observe(viewLifecycleOwner) { msg ->
            if (msg.isNotBlank()) Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        binding.findRecipesButton.setOnClickListener {
            viewModel.findMatchingRecipes()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
