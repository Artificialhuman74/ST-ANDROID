package com.example.sanathanatattva

import android.app.Application
import android.util.Base64
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.map.Mapper
import coil.request.Options
import com.example.sanathanatattva.data.api.ApiClient
import java.nio.ByteBuffer
import com.example.sanathanatattva.data.api.ApiService
import com.example.sanathanatattva.data.auth.TokenStore
import com.example.sanathanatattva.data.cache.ApiCache
import com.example.sanathanatattva.data.cart.CartStore
import com.example.sanathanatattva.data.prefs.ContainerPrefsStore
import org.maplibre.android.MapLibre

/** Manual DI: one container for the whole app. */
class AppContainer(app: Application) {
    val tokenStore = TokenStore(app)
    val cartStore = CartStore(app)
    val containerPrefs = ContainerPrefsStore(app)
    val luckyDrawPrefs = com.example.sanathanatattva.data.prefs.LuckyDrawPrefsStore(app)
    val cache = ApiCache(app)
    val api: ApiService = ApiClient.create(tokenStore).create(ApiService::class.java)
}

class App : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // The live order map (OpenStreetMap tiles, no API key).
        MapLibre.getInstance(this)
    }

    // Coil builds its own OkHttp client by default, which rejects the dev
    // backend's self-signed certificate; share the API client's trust config.
    // The backend also serves images as base64 data URIs, which Coil can't
    // load natively — the mapper decodes them into bytes Coil can handle.
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { ApiClient.imageHttpClient() }
            .components {
                add(object : Mapper<String, ByteBuffer> {
                    override fun map(data: String, options: Options): ByteBuffer? {
                        if (!data.startsWith("data:")) return null
                        val base64 = data.substringAfter("base64,", "")
                        if (base64.isEmpty()) return null
                        return runCatching {
                            ByteBuffer.wrap(Base64.decode(base64, Base64.DEFAULT))
                        }.getOrNull()
                    }
                })
            }
            .build()
}

/** Resolves relative backend image paths ("/uploads/x.jpg") to absolute URLs. */
fun imageUrl(path: String?): String? = when {
    path.isNullOrBlank() -> null
    path.startsWith("http") || path.startsWith("data:") -> path
    else -> BuildConfig.BASE_URL + path
}
