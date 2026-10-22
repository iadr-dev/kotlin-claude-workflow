plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.detekt.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.kover.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("quality") {
            id = "app.quality"
            implementationClass = "QualityConventionPlugin"
        }
    }
}
