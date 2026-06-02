package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GuestDao {
    @Query("SELECT * FROM guest_accounts ORDER BY timestamp DESC")
    fun getAllGuestAccounts(): Flow<List<GuestAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuestAccount(account: GuestAccount)

    @Delete
    suspend fun deleteGuestAccount(account: GuestAccount)

    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllActionLogs(): Flow<List<ActionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLog(log: ActionLog)

    @Query("DELETE FROM action_logs")
    suspend fun clearActionLogs()
}
