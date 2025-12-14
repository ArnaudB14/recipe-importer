package com.isariand.recettes

import androidx.appcompat.app.AppCompatActivity
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

class MainActivity : AppCompatActivity() {

    // 1. Initialisation du ViewModel (avec la Factory pour injecter le Repository et le DAO)
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {

                val GEMINI_API_KEY_SECRET = "AIzaSyCU2v3XBpYQK1yPfSZ8zJLf9kTtbfSyIYg"

                // 1. Dépendance API
                val apiService = RetrofitClient.apiService

                // 2. Dépendance DAO
                val recipeDao = AppDatabase.getDatabase(applicationContext).recipeDao()

                // 3. Création du Repository avec les DEUX dépendances
                val repository = VideoRepository(apiService, recipeDao, geminiApiKey = GEMINI_API_KEY_SECRET)

                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Utilisation du layout activity_main.xml qui contient SEULEMENT le FrameLayout conteneur
        setContentView(R.layout.activity_main)

        viewModel.javaClass

        val intentAction = intent.action
        val intentType = intent.type

        if (intentAction == Intent.ACTION_SEND && intentType == "text/plain") {
            // Cas 1: Partage reçu -> on traite le lien
            handleSharedIntent(intent)
        } else {
            // Cas 2: Lancement normal
            // S'assurer que le fragment est bien lancé une seule fois (pour éviter les doublons à la rotation)
            if (savedInstanceState == null) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, RecipeListFragment())
                    .commit()
            }
        }
    }

    /**
     * Traite l'Intent de partage: charge les détails, sauvegarde, puis affiche la liste.
     */
    private fun handleSharedIntent(intent: Intent) {
        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrEmpty()) {
            Toast.makeText(this, "Récupération des détails vidéo...", Toast.LENGTH_LONG).show()
            viewModel.loadVideoDetails(sharedText)

            // 💡 Observation pour la sauvegarde APRÈS le chargement
            // On utilise observeForever() ou on se contente d'observer une fois
            viewModel.videoData.observe(this) { data ->
                if (data != null && data.title?.isNotEmpty() == true) {
                    // Sauvegarder automatiquement après le chargement réussi
                    viewModel.saveLastFetchedVideo(sharedText)
                    Toast.makeText(this, "Recette chargée et sauvegardée!", Toast.LENGTH_LONG).show()

                    // Après la sauvegarde, basculer vers l'écran de la liste
                    navigateToRecipeList()

                    // 🛑 IMPORTANT: On retire l'observateur après le premier événement pour ne pas relancer
                    viewModel.videoData.removeObservers(this)
                }
            }

            // Observation d'erreur : juste un Toast
            viewModel.errorMessage.observe(this) { message ->
                if (message != null) {
                    Toast.makeText(this, "Erreur API : $message", Toast.LENGTH_LONG).show()
                    // En cas d'échec, on retourne quand même à la liste
                    navigateToRecipeList()

                    // 🛑 IMPORTANT: On retire l'observateur après le premier événement
                    viewModel.errorMessage.removeObservers(this)
                }
            }
        } else {
            // Si le lien partagé est vide, on affiche directement la liste
            navigateToRecipeList()
        }
    }

    /**
     * Fonction utilitaire pour lancer le Fragment de Liste.
     */
    private fun navigateToRecipeList() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, RecipeListFragment())
            .commit()
    }
}