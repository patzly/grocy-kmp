plugins {
  alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
  sourceSets {
    commonMain.dependencies {
      api(libs.compose.material3)
      // AppTheme, AppColor and AppContrast are parameters of GrocyTheme
      api(projects.core.model)
      implementation(projects.core.resources)
      implementation(libs.materialkolor)
    }
    androidMain.dependencies {
      implementation(libs.androidx.activity.compose)
    }
  }
}