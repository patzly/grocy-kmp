plugins {
    alias(libs.plugins.grocy.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // bookingRoute takes the BookingKind of core:model
            api(projects.core.model)
            api(projects.core.navigation)
        }
    }
}
