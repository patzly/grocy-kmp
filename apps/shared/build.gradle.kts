plugins {
    alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(projects.core.design)
            implementation(projects.core.navigation)
            implementation(projects.core.network)
            implementation(projects.feature.login.api)
            implementation(projects.feature.login.impl)
            implementation(projects.feature.start.api)
            implementation(projects.feature.start.impl)
        }
    }
}
