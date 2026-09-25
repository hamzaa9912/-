package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val vocalizedText: String,
    val partOfSpeech: String,
    val grammaticalRole: String,
    val grammaticalCase: String,
    val summaryIrab: String,
    val fullIrab: String,
    val root: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
