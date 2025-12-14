package com.isariand.recettes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Version de la base de données. Doit être incrémentée lors d'un changement de schéma.
@Database(entities = [RecipeEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Renommer l'ancienne colonne 'title' en 'videoTitle'
                db.execSQL("ALTER TABLE recipes RENAME COLUMN title TO videoTitle")
                // 2. Ajouter la nouvelle colonne 'customTitle'
                db.execSQL("ALTER TABLE recipes ADD COLUMN customTitle TEXT NOT NULL DEFAULT ''")
                // 3. Mettre à jour 'customTitle' pour prendre la valeur de l'ancien titre
                db.execSQL("UPDATE recipes SET customTitle = videoTitle")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Créer une nouvelle table temporaire avec la nouvelle structure
                db.execSQL("""
                    CREATE TABLE recipes_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        customTitle TEXT NOT NULL,
                        recipeTitle TEXT NOT NULL DEFAULT '',
                        description TEXT NOT NULL DEFAULT '',
                        ingredients TEXT NOT NULL DEFAULT '',
                        instructions TEXT NOT NULL DEFAULT '',
                        cookingTime TEXT,
                        dateAdded INTEGER NOT NULL,
                        videoUrl TEXT NOT NULL,
                        noWatermarkUrl TEXT,
                        videoTitle TEXT NOT NULL -- Gardons l'ancienne description TikTok pour l'historique si besoin
                    )
                """)

                // Copier les données (customTitle, dateAdded, videoUrl, etc.)
                db.execSQL("""
                    INSERT INTO recipes_new (
                        id, customTitle, dateAdded, videoUrl, noWatermarkUrl, videoTitle
                    )
                    SELECT 
                        id, customTitle, dateAdded, videoUrl, noWatermarkUrl, videoTitle
                    FROM recipes
                """)

                // Remplacer l'ancienne table
                db.execSQL("DROP TABLE recipes")
                db.execSQL("ALTER TABLE recipes_new RENAME TO recipes")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                INSTANCE = instance
                instance
            }
        }
    }
}