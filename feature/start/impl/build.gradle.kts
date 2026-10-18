plugins {
    alias(libs.plugins.grocy.kmp.compose)
    alias(libs.plugins.grocy.sqlite.host.tests)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // StartDependencies lists the repository of core:data
            api(projects.core.data)
            implementation(projects.core.design)
            implementation(projects.core.resources)
            implementation(projects.core.ui)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(projects.feature.about.api)
            implementation(projects.feature.masterdata.api)
            implementation(projects.feature.settings.api)
            implementation(projects.feature.start.api)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
