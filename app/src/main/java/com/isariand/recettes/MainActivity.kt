package com.isariand.recettes

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.isariand.recettes.repository.VideoRepository
import com.isariand.recettes.network.RetrofitClient
import com.isariand.recettes.viewmodel.MainViewModel
import android.content.Intent
import com.isariand.recettes.data.AppDatabase
import com.isariand.recettes.ui.RecipeListFragment
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {

                val GEMINI_API_KEY_SECRET = "AIzaSyCU2v3XBpYQK1yPfSZ8zJLf9kTtbfSyIYg"
                val apiService = RetrofitClient.apiService
                val recipeDao = AppDatabase.getDatabase(applicationContext).recipeDao()
                val repository = VideoRepository(apiService, recipeDao, geminiApiKey = GEMINI_API_KEY_SECRET)

                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView)
            .isAppearanceLightStatusBars = true

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        setContentView(R.layout.activity_main)

        val content = findViewById<android.view.View>(R.id.fragment_container)

        ViewCompat.setOnApplyWindowInsetsListener(content) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            v.setPadding(
                v.paddingLeft,
                sysBars.top,
                v.paddingRight,
                maxOf(sysBars.bottom, ime.bottom)
            )
            insets
        }
        viewModel.javaClass

        val intentAction = intent.action
        val intentType = intent.type

        if (intentAction == Intent.ACTION_SEND && intentType == "text/plain") {
            handleSharedIntent(intent)
        } else {
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, RecipeListFragment())
                    .commit()
            }
        }
    }

    private fun handleSharedIntent(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrEmpty()) {
            Toast.makeText(this, "Récupération des détails vidéo...", Toast.LENGTH_LONG).show()
            viewModel.loadVideoDetails(sharedText)
            viewModel.videoData.observe(this) { data ->
                if (data != null && data.title?.isNotEmpty() == true) {
                    viewModel.saveLastFetchedVideo(sharedText)
                    Toast.makeText(this, "Recette chargée et sauvegardée!", Toast.LENGTH_LONG).show()
                    navigateToRecipeList()
                    viewModel.videoData.removeObservers(this)
                }
            }

            viewModel.errorMessage.observe(this) { message ->
                if (message != null) {
                    Toast.makeText(this, "Erreur API : $message", Toast.LENGTH_LONG).show()
                    navigateToRecipeList()
                    viewModel.errorMessage.removeObservers(this)
                }
            }
        } else {
            navigateToRecipeList()
        }
    }

    private fun navigateToRecipeList() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, RecipeListFragment())
            .commit()
    }
}