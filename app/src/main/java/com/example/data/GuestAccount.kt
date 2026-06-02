package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "guest_accounts")
data class GuestAccount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: String,
    val uid: String,
    val pass: String,
    val label: String,
    val timestamp: Long = System.currentTimeMillis()
)
