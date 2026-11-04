plugins {
    alias(libs.plugins.grocy.kmp.library)
    alias(libs.plugins.grocy.sqlite.host.tests)
}

// Closed source (Google Play Services), so only the Play app and the watch may depend on it
kotlin {
    sourceSets {
        commonMain.dependencies {
            // The view model of the watch takes the repositories
            api(projects.core.data)
            api(libs.jetbrains.lifecycle.viewmodel.compose)
        }
        androidMain.dependencies {
            implementation(libs.play.services.wearable)
            implementation(libs.kotlinx.coroutines.play.services)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
