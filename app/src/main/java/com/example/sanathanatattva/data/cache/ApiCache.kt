package com.example.sanathanatattva.data.cache

import android.content.Context
import com.example.sanathanatattva.data.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.io.File
import java.security.MessageDigest

/**
 * How long a saved answer stands in for the server. Tabs that show slow-moving data
 * (the catalogue, containers) open from the phone and only ask again after this long;
 * live data (orders, wallet, the draw) never goes through this cache.
 */
const val CACHE_MAX_AGE_MS = 3 * 60 * 60 * 1000L

data class Cached<T>(val value: T, val savedAt: Long) {
    val isFresh: Boolean get() = isFresh(savedAt)
}

fun isFresh(savedAt: Long): Boolean =
    System.currentTimeMillis() - savedAt in 0 until CACHE_MAX_AGE_MS

/**
 * Server answers kept on the phone, so a tab opens drawn from its last answer instead
 * of waiting on the network, even after the app was closed. Personal answers are
 * stamped with whose login they came from, and a different login never reads them.
 * Lives in noBackupFilesDir, so none of it leaves the phone in a backup.
 */
class ApiCache(context: Context) {
    private val dir = File(context.noBackupFilesDir, "api-cache")
    private val json = ApiClient.json

    @Serializable
    private data class Entry(val savedAt: Long, val owner: String? = null, val value: JsonElement)

    suspend fun <T> read(key: String, serializer: KSerializer<T>, owner: String? = null): Cached<T>? =
        withContext(Dispatchers.IO) {
            runCatching {
                val file = File(dir, "$key.json")
                if (!file.exists()) return@runCatching null
                val entry = json.decodeFromString(Entry.serializer(), file.readText())
                if (entry.owner != owner) return@runCatching null
                Cached(json.decodeFromJsonElement(serializer, entry.value), entry.savedAt)
            }.getOrNull()
        }

    suspend fun <T> write(key: String, serializer: KSerializer<T>, value: T, owner: String? = null) {
        withContext(Dispatchers.IO) {
            runCatching {
                dir.mkdirs()
                val entry = Entry(System.currentTimeMillis(), owner, json.encodeToJsonElement(serializer, value))
                // Written aside then swapped in, so a crash mid-write never leaves half a file.
                val tmp = File(dir, "$key.json.tmp")
                tmp.writeText(json.encodeToString(Entry.serializer(), entry))
                tmp.renameTo(File(dir, "$key.json"))
            }
        }
    }

    suspend fun clear(key: String) {
        withContext(Dispatchers.IO) { File(dir, "$key.json").delete() }
    }

    companion object {
        /** Cache keys. */
        const val CATALOGUE = "catalogue"
        const val CONTAINERS = "containers"

        /** Answers that belong to one account; dropped from the phone on logout. */
        val PERSONAL = listOf(CONTAINERS)

        /** A stand-in for the login that owns an answer, so the token itself is never written out. */
        fun ownerOf(token: String?): String? = token?.let {
            MessageDigest.getInstance("SHA-256").digest(it.toByteArray())
                .take(12).joinToString("") { b -> "%02x".format(b) }
        }
    }
}
