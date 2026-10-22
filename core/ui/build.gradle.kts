plugins {
  alias(libs.plugins.android.library)
}

android {
    namespace = "com.example.pins.ui"
    compileSdk = 36
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
  implementation(project(":core:domain"))
  implementation(libs.androidx.core.ktx)
  testImplementation(libs.junit)
}
