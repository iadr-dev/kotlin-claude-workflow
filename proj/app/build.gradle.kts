plugins {
    alias(libs.plugins.android.application)
    id("app.quality")
}

android {
    namespace = "com.example.app"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    testImplementation(libs.junit)
}
