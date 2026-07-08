package com.example.sanathanatattva

import android.app.Application
import com.example.sanathanatattva.data.api.ApiClient
import com.example.sanathanatattva.data.api.ApiService
import com.example.sanathanatattva.data.auth.TokenStore
import com.example.sanathanatattva.data.cart.CartStore
import com.example.sanathanatattva.data.prefs.ContainerPrefsStore

/** Manual DI: one container for the whole app. */
class AppContainer(app: Application) {
    val tokenStore = TokenStore(app)
    val cartStore = CartStore(app)
    val containerPrefs = ContainerPrefsStore(app)
    val api: ApiService = ApiClient.create(tokenStore).create(ApiService::class.java)
}

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Resolves relative backend image paths ("/uploads/x.jpg") to absolute URLs. */
fun imageUrl(path: String?): String? = when {
    path.isNullOrBlank() -> null
    path.startsWith("http") || path.startsWith("data:") -> path
    else -> BuildConfig.BASE_URL + path
}
