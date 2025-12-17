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
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.isariand.recettes.data.AppDatabase
import com.isariand.recettes.ui.RecipeListFragment
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.isariand.recettes.BuildConfig
import com.isariand.recettes.network.GroqClient

class MainActivity : FragmentActivity() {

    private var lastSharedText: String? = null
    private var lastSharedAt: Long = 0L

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {

                val apiService = RetrofitClient.apiService
                val recipeDao = AppDatabase.getDatabase(applicationContext).recipeDao()
                val repository = VideoRepository(apiService, recipeDao, groq = GroqClient.api)

                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!isTaskRoot
            && intent?.action == Intent.ACTION_MAIN
            && intent?.hasCategory(Intent.CATEGORY_LAUNCHER) == true
        ) {
            finish()
            return
        }
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
        val overlay = findViewById<View>(R.id.importOverlay)
        val spinner = findViewById<View>(R.id.importSpinner)
        val icon = findViewById<ImageView>(R.id.importResultIcon)
        val text = findViewById<TextView>(R.id.importText)

        viewModel.importState.observe(this) { state ->
            when (state) {
                is MainViewModel.ImportState.Idle -> {
                    overlay.visibility = View.GONE
                }

                is MainViewModel.ImportState.Loading -> {
                    overlay.visibility = View.VISIBLE
                    spinner.visibility = View.VISIBLE
                    icon.visibility = View.GONE
                    text.text = state.message
                }

                is MainViewModel.ImportState.Success -> {
                    overlay.visibility = View.VISIBLE
                    spinner.visibility = View.GONE
                    icon.visibility = View.VISIBLE
                    icon.setImageResource(R.drawable.ic_check_sketch)
                    text.text = state.message

                    overlay.postDelayed({
                        overlay.visibility = View.GONE
                    }, 1200)
                }

                is MainViewModel.ImportState.Error -> {
                    overlay.visibility = View.VISIBLE
                    spinner.visibility = View.GONE
                    icon.visibility = View.VISIBLE
                    icon.setImageResource(R.drawable.ic_close_sketch)
                    text.text = state.message

                    overlay.postDelayed({
                        overlay.visibility = View.GONE
                    }, 1800)
                }
            }
        }

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
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
        val now = System.currentTimeMillis()
        if (sharedText == lastSharedText && (now - lastSharedAt) < 2000) {
            navigateToRecipeList()
            return
        }
        lastSharedText = sharedText
        lastSharedAt = now


        viewModel.importFromTiktok(sharedText)

        navigateToRecipeList()
    }


    private fun navigateToRecipeList() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, RecipeListFragment())
            .commit()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // TRÈS IMPORTANT : met à jour l'intent de l'activité

        val intentAction = intent.action
        val intentType = intent.type

        if (intentAction == Intent.ACTION_SEND && intentType == "text/plain") {
            handleSharedIntent(intent)
        }
    }
}