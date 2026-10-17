plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.qbao.tweak"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.qbao.tweak"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
