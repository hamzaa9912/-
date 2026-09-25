package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT * FROM favorites WHERE query = :query LIMIT 1")
    suspend fun getFavoriteByQuery(query: String): FavoriteEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE query = :query LIMIT 1)")
    fun isFavoriteFlow(query: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity): Long

    @Update
    suspend fun updateFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE query = :query")
    suspend fun deleteFavoriteByQuery(query: String)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)

    @Query("SELECT * FROM favorites WHERE query LIKE '%' || :keyword || '%' OR fullIrab LIKE '%' || :keyword || '%' OR notes LIKE '%' || :keyword || '%' ORDER BY timestamp DESC")
    fun searchFavorites(keyword: String): Flow<List<FavoriteEntity>>
}
