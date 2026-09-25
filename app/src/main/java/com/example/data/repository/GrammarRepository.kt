package com.example.data.repository

import com.example.data.db.FavoriteDao
import com.example.data.db.FavoriteEntity
import com.example.data.db.SearchHistoryDao
import com.example.data.db.SearchHistoryEntity
import kotlinx.coroutines.flow.Flow

class GrammarRepository(
    private val favoriteDao: FavoriteDao,
    private val searchHistoryDao: SearchHistoryDao
) {
    val allFavorites: Flow<List<FavoriteEntity>> = favoriteDao.getAllFavorites()
    val recentHistory: Flow<List<SearchHistoryEntity>> = searchHistoryDao.getRecentHistory()

    fun isFavorite(query: String): Flow<Boolean> = favoriteDao.isFavoriteFlow(query)

    fun searchFavorites(keyword: String): Flow<List<FavoriteEntity>> =
        favoriteDao.searchFavorites(keyword)

    suspend fun addFavorite(favorite: FavoriteEntity): Long =
        favoriteDao.insertFavorite(favorite)

    suspend fun removeFavoriteByQuery(query: String) =
        favoriteDao.deleteFavoriteByQuery(query)

    suspend fun removeFavoriteById(id: Long) =
        favoriteDao.deleteFavoriteById(id)

    suspend fun updateFavorite(favorite: FavoriteEntity) =
        favoriteDao.updateFavorite(favorite)

    suspend fun addSearchHistory(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            searchHistoryDao.deleteByQuery(trimmed)
            searchHistoryDao.insertHistory(SearchHistoryEntity(query = trimmed))
        }
    }

    suspend fun deleteHistoryItem(id: Long) =
        searchHistoryDao.deleteById(id)

    suspend fun clearHistory() =
        searchHistoryDao.clearAllHistory()
}
