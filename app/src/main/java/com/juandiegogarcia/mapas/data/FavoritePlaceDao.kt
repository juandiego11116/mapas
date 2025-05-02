package com.juandiegogarcia.mapas.data

import androidx.room.*
import com.juandiegogarcia.mapas.model.FavoritePlace
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: FavoritePlace)

    @Query("SELECT * FROM favorites")
    fun getAllFlow(): Flow<List<FavoritePlace>>
    @Delete
    fun delete(place: FavoritePlace)
}
