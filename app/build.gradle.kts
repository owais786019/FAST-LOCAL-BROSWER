plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.local.browser"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.local.browser"
        // androidx.webkit 1.16.0 requires minSdk 24.
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // 1.16.0+: stable startUpWebView API for async WebView startup.
    implementation("androidx.webkit:webkit:1.16.0")
}
