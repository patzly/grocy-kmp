plugins {
    alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.design)
            implementation(projects.core.navigation)
            implementation(projects.feature.start.api)
            implementation(projects.feature.start.impl)
        }
    }
}
