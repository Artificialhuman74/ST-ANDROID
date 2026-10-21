import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// Release signing lives outside the repo entirely — see keystore.properties.example.
// Builds fall back to unsigned when it's absent (e.g. a fresh checkout) so local/CI
// builds don't hard-fail; only a real release upload needs it populated.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseSigning = keystorePropertiesFile.exists()
if (hasReleaseSigning) {
    FileInputStream(keystorePropertiesFile).use { keystoreProperties.load(it) }
}

android {
    namespace = "com.example.sanathanatattva"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.sanathanatattva.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 8
        versionName = "7.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Railway-hosted backend (valid TLS cert, no trust-all needed).
            // For a local backend, use https://10.0.2.2:5001 with TRUST_ALL_CERTS = true.
            buildConfigField("String", "BASE_URL", "\"https://sanathanatattava-production-67b1.up.railway.app\"")
            buildConfigField("boolean", "TRUST_ALL_CERTS", "false")
        }
        release {
            // Points at the live Railway backend (valid TLS cert). The vanity domain
            // api.sanathanatattva.shop is not yet provisioned — it returns NXDOMAIN, which
            // is why release builds (Play Store internal testing) couldn't reach the API.
            // Swap back to the vanity domain once its DNS/cert are actually live.
            buildConfigField("String", "BASE_URL", "\"https://sanathanatattava-production-67b1.up.railway.app\"")
            buildConfigField("boolean", "TRUST_ALL_CERTS", "false")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // R8 shrinks, optimizes and obfuscates the release DEX (Play flags builds
            // whose obfuscation falls under 25%). Libraries ship their own keep rules;
            // app-specific ones live in src/main/keepRules. The mapping file is packed
            // into the AAB, so Play still shows readable crash stack traces.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "src/main/keepRules/rules.keep",
            )
            // Package native debug symbols (from Razorpay etc.) into the AAB so Play
            // can symbolicate native crashes/ANRs — clears the "no debug symbols" warning.
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.razorpay.checkout)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services)
    implementation(libs.googleid)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
