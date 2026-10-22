plugins {
    alias(libs.plugins.kotlin.jvm)
    id("app.quality")
}

dependencies {
    testImplementation(libs.junit)
}
