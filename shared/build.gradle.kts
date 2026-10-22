plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}
kotlin {
    jvm()
    android {
        namespace = "com.example.shared"
        compileSdk = 36
        minSdk = 24
        withHostTest { }
    }
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    sourceSets {
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
