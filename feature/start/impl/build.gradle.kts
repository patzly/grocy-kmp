plugins {
    alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.design)
            implementation(projects.core.resources)
            implementation(projects.feature.about.api)
            implementation(projects.feature.start.api)
        }
    }
}
