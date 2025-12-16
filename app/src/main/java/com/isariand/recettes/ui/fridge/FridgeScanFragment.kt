package com.isariand.recettes.ui.fridge

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.isariand.recettes.BuildConfig
import com.isariand.recettes.R
import com.isariand.recettes.data.AppDatabase
import com.isariand.recettes.databinding.FragmentFridgeScanBinding
import com.isariand.recettes.network.RetrofitClient
import com.isariand.recettes.repository.VideoRepository

class FridgeScanFragment : Fragment(R.layout.fragment_fridge_scan) {

    private var _binding: FragmentFridgeScanBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FridgeScanViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val api = RetrofitClient.apiService
                val dao = AppDatabase.getDatabase(requireContext()).recipeDao()
                val repo = VideoRepository(api, dao, geminiApiKey = BuildConfig.GEMINI_API_KEY)
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
