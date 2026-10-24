plugins {
    alias(libs.plugins.grocy.kmp.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.jetbrains.navigation3.ui)
            implementation(projects.core.design)
            implementation(libs.compose.material3.adaptive.navigation3)
            implementation(libs.jetbrains.lifecycle.viewmodel.navigation3)
            implementation(libs.jetbrains.navigationevent.compose)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}