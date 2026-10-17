plugins {
    alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(projects.core.design)
            implementation(projects.core.resources)
        }
        androidMain.dependencies {
            // The photo picker of rememberImagePicker
            implementation(libs.androidx.activity.compose)
        }
    }
}
