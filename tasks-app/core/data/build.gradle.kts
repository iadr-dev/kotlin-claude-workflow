plugins {
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  jvmToolchain(17)
  explicitApi()
}

dependencies {
  implementation(project(":core:domain"))

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
