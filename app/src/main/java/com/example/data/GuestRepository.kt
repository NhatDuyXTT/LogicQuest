package com.example.data

import kotlinx.coroutines.flow.Flow

class GuestRepository(private val guestDao: GuestDao) {
    val allGuestAccounts: Flow<List<GuestAccount>> = guestDao.getAllGuestAccounts()
    val allActionLogs: Flow<List<ActionLog>> = guestDao.getAllActionLogs()

    suspend fun insertAccount(account: GuestAccount) {
        guestDao.insertGuestAccount(account)
    }

    suspend fun deleteAccount(account: GuestAccount) {
        guestDao.deleteGuestAccount(account)
    }

    suspend fun insertLog(log: ActionLog) {
        guestDao.insertActionLog(log)
    }

    suspend fun clearLogs() {
        guestDao.clearActionLogs()
    }
}
