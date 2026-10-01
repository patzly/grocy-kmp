plugins {
  alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      api(libs.compose.material3)
    }
  }
}