plugins {
  alias(libs.plugins.grocy.web.application)
}

kotlin {
  sourceSets {
    wasmJsMain.dependencies {
      implementation(projects.core.design)
      implementation(projects.core.navigation)
      implementation(projects.feature.start.api)
      implementation(projects.feature.start.impl)
    }
  }
}