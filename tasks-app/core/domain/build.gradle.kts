plugins {
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  jvmToolchain(17)
  explicitApi()
}

dependencies {
  api(libs.kotlinx.coroutines.core)

  testImplementation(project(":core:testing"))
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
}
