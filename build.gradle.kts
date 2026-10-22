import io.gitlab.arturbosch.detekt.Detekt

plugins {
    kotlin("jvm") version "2.4.21"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

dependencies { testImplementation(kotlin("test")) }

tasks.test { useJUnitPlatform() }

detekt {
    // -Pdetekt.badConfig=true points at a config with 2.x keys to prove that 1.x validates keys
    val bad = providers.gradleProperty("detekt.badConfig").isPresent
    config.setFrom(file(if (bad) "config/detekt/detekt-bad.yml" else "config/detekt/detekt.yml"))
    buildUponDefaultConfig = true // 1.x properties use "=", 2.x uses .set()
}

// `check` only runs plain `detekt` by default; attach the type-resolution tasks too.
tasks.named("check") { dependsOn(tasks.withType<Detekt>()) }
