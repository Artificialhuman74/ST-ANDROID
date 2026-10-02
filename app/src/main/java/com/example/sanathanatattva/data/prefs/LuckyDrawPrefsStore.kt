package com.example.sanathanatattva.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.drawPrefs by preferencesDataStore(name = "lucky_draw")

/**
 * The once-per-draw moments this device remembers, each keyed per scheme and month
 * (a round holds twelve draws and each is its own moment).
 */
enum class DrawMark(internal val prefix: String) {
    /** The full-screen reveal was watched to the end. */
    REVEAL("seen"),

    /** The home page's "someone else won" card was dismissed. */
    RESULT_INFO("info"),

    /** The gold pass that marks entering this month's draw has played. */
    ENTERED("entered"),
}

interface DrawSeenStore {
    suspend fun hasSeen(consumerId: Long, schemeId: Long, month: String, mark: DrawMark = DrawMark.REVEAL): Boolean
    suspend fun markSeen(consumerId: Long, schemeId: Long, month: String, mark: DrawMark = DrawMark.REVEAL)
}

class LuckyDrawPrefsStore(context: Context) : DrawSeenStore {
    private val store = context.applicationContext.drawPrefs
    // Accounts sharing a device must each see their own result.
    private fun key(consumerId: Long, schemeId: Long, month: String, mark: DrawMark) =
        booleanPreferencesKey("${mark.prefix}_${consumerId}_${schemeId}_$month")

    override suspend fun hasSeen(consumerId: Long, schemeId: Long, month: String, mark: DrawMark): Boolean =
        store.data.first()[key(consumerId, schemeId, month, mark)] == true

    override suspend fun markSeen(consumerId: Long, schemeId: Long, month: String, mark: DrawMark) {
        store.edit { it[key(consumerId, schemeId, month, mark)] = true }
    }
}
