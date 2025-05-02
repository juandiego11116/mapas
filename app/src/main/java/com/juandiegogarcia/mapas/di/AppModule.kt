package com.juandiegogarcia.mapas.di

import android.content.Context
import com.juandiegogarcia.mapas.data.AppDatabase
import com.juandiegogarcia.mapas.data.FavoritePlaceDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * Dagger Hilt module that provides application-wide dependencies.
 * It tells Hilt how to create instances of AppDatabase and FavoritePlaceDao.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides a singleton instance of AppDatabase.
     *
     * @param context The application context, injected by Hilt.
     * @return An instance of AppDatabase.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    /**
     * Provides an instance of FavoritePlaceDao using the AppDatabase.
     *
     * @param db The database instance injected by Hilt.
     * @return The FavoritePlaceDao from the database.
     */
    @Provides
    fun provideFavoritePlaceDao(db: AppDatabase): FavoritePlaceDao {
        return db.favoritePlaceDao()
    }
}
