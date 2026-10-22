plugins {
  alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.pins"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.pins"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
  implementation(project(":core:ui"))
  implementation(project(":core:domain"))
  implementation(libs.androidx.core.ktx)
}
