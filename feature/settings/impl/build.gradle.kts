plugins {
    alias(libs.plugins.grocy.kmp.compose)
    alias(libs.plugins.grocy.sqlite.host.tests)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // SettingsDependencies lists repositories and stores of core:data
            api(projects.core.data)
            implementation(projects.core.design)
            implementation(projects.core.forms)
            implementation(projects.core.resources)
            implementation(projects.core.ui)
            implementation(projects.feature.about.api)
            implementation(projects.feature.settings.api)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
        }
        androidMain.dependencies {
            // Asks for the permission and reads whether notifications are allowed
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
