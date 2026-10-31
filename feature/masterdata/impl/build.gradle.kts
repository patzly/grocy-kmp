plugins {
    alias(libs.plugins.grocy.kmp.compose)
    alias(libs.plugins.grocy.sqlite.host.tests)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // MasterDataDependencies lists the repository of core:data
            api(projects.core.data)
            implementation(projects.core.design)
            implementation(projects.core.forms)
            implementation(projects.core.productcard)
            implementation(projects.core.resources)
            implementation(projects.core.scanner)
            implementation(projects.core.ui)
            implementation(projects.feature.masterdata.api)
            implementation(projects.feature.settings.api)
            implementation(projects.feature.start.api)
            implementation(projects.feature.stock.api)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.navigationevent.compose)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
