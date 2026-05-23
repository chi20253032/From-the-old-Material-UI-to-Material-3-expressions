package com.example.converter

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversion_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val xmlSize: Int,
    val activitiesCount: Int,
    val conversionTime: Long,
    val accuracyScore: Int,
    val originalXml: String,
    val convertedComposeCode: String
)
