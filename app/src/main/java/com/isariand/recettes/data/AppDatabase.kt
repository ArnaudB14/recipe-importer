package com.isariand.recettes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// Version de la base de données. Doit être incrémentée lors d'un changement de schéma.
@Database(entities = [RecipeEntity::class], version = 2, exportSchema = false)
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe_database"
                ).addMigrations(MIGRATION_1_2).build()
                INSTANCE = instance
                instance
            }
        }
    }
}