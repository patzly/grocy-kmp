plugins {
  alias(libs.plugins.grocy.android.application)
}

android {
  defaultConfig {
    minSdk {
      version = release(libs.versions.android.wear.minSdk.get().toInt())
    }
    // Must differ from the phone app, both share one Play Store listing
    versionCode = providers.gradleProperty("grocy.versionCode").get().toInt() + 1_000_000
  }
}

dependencies {
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.wear.compose.foundation)
  implementation(libs.androidx.wear.compose.material3)
}