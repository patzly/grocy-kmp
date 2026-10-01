plugins {
    alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.start.api)
            implementation(projects.core.design)
        }
    }
}