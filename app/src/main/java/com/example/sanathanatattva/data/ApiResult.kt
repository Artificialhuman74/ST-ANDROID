package com.example.sanathanatattva.data

import com.example.sanathanatattva.data.api.ApiClient
import com.example.sanathanatattva.data.model.ApiError
import retrofit2.HttpException
import java.io.IOException

/**
 * Runs an API call and maps failures to messages that name the fix.
 * The backend's error shape is `{ "error": "..." }`.
 */
suspend fun <T> apiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: HttpException) {
    val message = e.response()?.errorBody()?.string()?.let { raw ->
        runCatching { ApiClient.json.decodeFromString<ApiError>(raw).error }.getOrNull()
    } ?: when (e.code()) {
        401 -> "Your session has expired. Please log in again."
        else -> "Something went wrong on the server (error ${e.code()})."
    }
    Result.failure(ApiException(message, e.code()))
} catch (e: IOException) {
    Result.failure(
        ApiException("Couldn't reach the server. Check your connection and try again.", null)
    )
}

class ApiException(override val message: String, val code: Int?) : Exception(message)
