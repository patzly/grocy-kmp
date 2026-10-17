plugins {
    alias(libs.plugins.grocy.kmp.library)
    alias(libs.plugins.grocy.sqlite.host.tests)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // GrocyApi and Flow are part of the public API (AuthRepository)
            api(projects.core.network)
            // MasterDataRepository takes the GrocyDatabase
            api(projects.core.database)
            api(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.ktor.client.mock)
        }
    }
}
