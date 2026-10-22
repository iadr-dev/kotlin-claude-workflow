plugins {
  alias(libs.plugins.kotlin.jvm)
}

kotlin {
  explicitApi()
}

dependencies {
  testImplementation(libs.junit)
}
