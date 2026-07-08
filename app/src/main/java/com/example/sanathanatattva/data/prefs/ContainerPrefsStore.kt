package com.example.sanathanatattva.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.containerPrefsDataStore by preferencesDataStore(name = "container_prefs")

class ContainerPrefsStore(private val context: Context) {

    private val steelSheetSeenKey = booleanPreferencesKey("steel_sheet_seen")

    val steelSheetSeen: Flow<Boolean> = context.containerPrefsDataStore.data.map {
        it[steelSheetSeenKey] ?: false
    }

    suspend fun markSteelSheetSeen() {
        context.containerPrefsDataStore.edit { it[steelSheetSeenKey] = true }
    }
}
