package com.isariand.recettes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [RecipeEntity::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes RENAME COLUMN title TO videoTitle")
                db.execSQL("ALTER TABLE recipes ADD COLUMN customTitle TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE recipes SET customTitle = videoTitle")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
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

                db.execSQL("""
                    INSERT INTO recipes_new (
                        id, customTitle, dateAdded, videoUrl, noWatermarkUrl, videoTitle
                    )
                    SELECT 
                        id, customTitle, dateAdded, videoUrl, noWatermarkUrl, videoTitle
                    FROM recipes
                """)

                db.execSQL("DROP TABLE recipes")
                db.execSQL("ALTER TABLE recipes_new RENAME TO recipes")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            ALTER TABLE recipes
            ADD COLUMN tags TEXT NOT NULL DEFAULT ''
            """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipes ADD COLUMN macros TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
                INSTANCE = instance
                instance
            }
        }
    }
}