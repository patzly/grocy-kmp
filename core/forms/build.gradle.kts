plugins {
    alias(libs.plugins.grocy.kmp.compose)
    alias(libs.plugins.grocy.sqlite.host.tests)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Forms take models and repositories of core:data (userfields, units, object actions)
            api(projects.core.data)
            implementation(projects.core.design)
            implementation(projects.core.navigation)
            implementation(projects.core.resources)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.navigationevent.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
