package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_logs")
data class ActionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
