plugins {
    alias(libs.plugins.grocy.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.navigation)
            // CalendarRoute takes the view of core:model
            api(projects.core.model)
        }
    }
}
