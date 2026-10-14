package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_history")
data class ConnectionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val serverName: String,
    val countryCode: String,
    val durationSeconds: Long,
    val bytesIn: Long,
    val bytesOut: Long,
    val protocol: String
)
