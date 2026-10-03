plugins {
    alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
    android {
        // The KMP Android library plugin skips assets by default, Compose Resources live there
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.components.resources)
        }
    }
}

compose.resources {
    // Res is used by every feature module, not only inside this one
    publicResClass = true
    packageOfResClass = "com.patrickzedler.grocy.core.resources"
}
