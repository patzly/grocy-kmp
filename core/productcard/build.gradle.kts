plugins {
    alias(libs.plugins.grocy.kmp.compose)
    alias(libs.plugins.grocy.sqlite.host.tests)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // The product overview and its forms take the repositories of core:data
            api(projects.core.data)
            implementation(projects.core.design)
            implementation(projects.core.forms)
            implementation(projects.core.resources)
            implementation(projects.core.scanner)
            implementation(projects.core.ui)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
