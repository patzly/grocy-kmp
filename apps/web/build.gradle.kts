plugins {
  alias(libs.plugins.grocy.web.application)
}

kotlin {
  sourceSets {
    wasmJsMain.dependencies {
      implementation(projects.apps.shared)
    }
  }
}