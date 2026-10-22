plugins {
    alias(libs.plugins.android.library)
    id("app.quality")
}

android {
    namespace = "com.example.app.ui"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
}

dependencies {
    testImplementation(libs.junit)
}
