package com.juandiegogarcia.mapas.data

import androidx.room.*
import com.juandiegogarcia.mapas.model.FavoritePlace
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the FavoritePlace entity.
 * Defines the database operations related to the "favorites" table.
 */
@Dao
interface FavoritePlaceDao {

    /**
     * Inserts a FavoritePlace into the database.
     * If the place already exists (same primary key), it will be replaced.
     *
     * @param place The FavoritePlace to insert or update.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: FavoritePlace)

    /**
     * Retrieves all favorite places as a Flow, allowing real-time updates.
     *
     * @return A Flow that emits the list of favorite places from the database.
     */
    @Query("SELECT * FROM favorites")
    fun getAllFlow(): Flow<List<FavoritePlace>>

    /**
     * Deletes a specific FavoritePlace from the database.
     *
     * @param place The FavoritePlace to remove.
     */
    @Delete
    fun delete(place: FavoritePlace)
}
