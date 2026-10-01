plugins {
  alias(libs.plugins.grocy.android.application)
}

dependencies {
  implementation(projects.core.design)
  implementation(projects.core.navigation)
  implementation(projects.feature.start.api)
  implementation(projects.feature.start.impl)

  implementation(libs.androidx.activity.compose)
}