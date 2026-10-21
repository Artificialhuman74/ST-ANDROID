package com.example.sanathanatattva.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.example.sanathanatattva.BuildConfig
import com.example.sanathanatattva.data.api.ApiService
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull

/** A newer build to offer at launch. [required]: this build is below the server's minimum. */
data class UpdateOffer(val required: Boolean)

/**
 * Asks the Play Store whether a newer version is live, and our server whether this build is
 * still allowed (admin → Settings → App updates). Either can fail quietly: a sideloaded or
 * debug build has no Play answer, and a phone offline at launch just isn't asked.
 */
suspend fun checkForUpdate(context: Context, api: ApiService): UpdateOffer? = coroutineScope {
    val play = async {
        withTimeoutOrNull(6_000) {
            runCatching {
                AppUpdateManagerFactory.create(context).requestAppUpdateInfo()
                    .updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            }.getOrNull()
        } ?: false
    }
    val minimum = async {
        withTimeoutOrNull(6_000) { apiCall { api.appVersions() }.getOrNull()?.consumer?.min_version_code } ?: 0
    }
    val required = BuildConfig.VERSION_CODE < minimum.await()
    when {
        required -> UpdateOffer(required = true)
        play.await() -> UpdateOffer(required = false)
        else -> null
    }
}

/** This app's Play Store page (the Play app if it's there, else the website). */
fun openPlayStore(context: Context) {
    val id = context.packageName
    val market = Intent(Intent.ACTION_VIEW, "market://details?id=$id".toUri())
        .setPackage("com.android.vending")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$id".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
