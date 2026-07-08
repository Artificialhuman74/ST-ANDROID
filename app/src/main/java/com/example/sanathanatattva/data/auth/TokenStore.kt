package com.example.sanathanatattva.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * Persists the consumer JWT (the Android equivalent of the web app's
 * `consumer_token` in localStorage).
 */
class TokenStore(private val context: Context) {

    private val tokenKey = stringPreferencesKey("consumer_token")

    val token: Flow<String?> = context.authDataStore.data.map { it[tokenKey] }

    suspend fun current(): String? = token.first()

    suspend fun save(token: String) {
        context.authDataStore.edit { it[tokenKey] = token }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.remove(tokenKey) }
    }
}
