plugins {
    alias(libs.plugins.grocy.kmp.library)
}

// Closed source (Google Play), so only the Play app may depend on this module
kotlin {
    sourceSets {
        commonMain.dependencies {
            // PlayAppUpdater implements AppUpdater and is handed to the app as such
            api(projects.core.data)
        }
        androidMain.dependencies {
            implementation(libs.play.app.update.ktx)
        }
    }
}
