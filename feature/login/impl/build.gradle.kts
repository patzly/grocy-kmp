plugins {
    alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // AuthRepository is part of LoginDependencies
            api(projects.core.data)
            implementation(projects.core.design)
            implementation(projects.core.resources)
            implementation(projects.feature.login.api)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
        }
    }
}
