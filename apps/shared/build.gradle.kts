plugins {
    alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
    android {
        // Launcher icon, themes and backup rules, shared by both phone apps
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.database)
            implementation(projects.core.design)
            implementation(projects.core.navigation)
            implementation(projects.core.network)
            implementation(projects.core.ui)
            // PlatformDependencies exposes the barcode decoders of the platform by engine
            api(projects.core.scanner)
            api(projects.core.model)
            implementation(projects.feature.login.api)
            implementation(projects.feature.login.impl)
            implementation(projects.feature.masterdata.api)
            implementation(projects.feature.masterdata.impl)
            implementation(projects.feature.start.api)
            implementation(projects.feature.start.impl)
            implementation(projects.feature.about.api)
            implementation(projects.feature.about.impl)
            implementation(projects.feature.settings.api)
            implementation(projects.feature.settings.impl)
            implementation(projects.feature.shoppinglist.api)
            implementation(projects.feature.stock.api)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.splashscreen)
            // A popup closing during a predictive back gesture crashed older versions
            implementation(libs.androidx.navigationevent.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
