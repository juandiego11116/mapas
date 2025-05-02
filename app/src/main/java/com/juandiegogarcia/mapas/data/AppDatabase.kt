package com.juandiegogarcia.mapas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.juandiegogarcia.mapas.model.FavoritePlace

/**
 * Main Room database class for the application.
 * Defines the database configuration and serves as the app's main access point to persisted data.
 */
@Database(entities = [FavoritePlace::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    // Returns the DAO used to access the FavoritePlace table.
    abstract fun favoritePlaceDao(): FavoritePlaceDao

    companion object {
        // Singleton instance of the database to prevent multiple instances being opened at the same time.
        @Volatile private var INSTANCE: AppDatabase? = null

        /**
         * Returns the singleton instance of AppDatabase.
         * If it doesn't exist yet, it creates a new one.
         *
         * @param context Application context.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "favorite_places.db" // Name of the SQLite database file.
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
