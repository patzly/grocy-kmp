plugins {
  alias(libs.plugins.grocy.android.application)
}

dependencies {
  implementation(projects.apps.shared)

  implementation(libs.androidx.activity.compose)
}