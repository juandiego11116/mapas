package com.juandiegogarcia.mapas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.juandiegogarcia.mapas.model.FavoritePlace

@Database(entities = [FavoritePlace::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun favoritePlaceDao(): FavoritePlaceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "favorite_places.db"
                )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
