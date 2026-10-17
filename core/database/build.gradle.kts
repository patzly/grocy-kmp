plugins {
    alias(libs.plugins.grocy.kmp.library)
    alias(libs.plugins.grocy.room)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Models and Flow are part of the public API (MasterDataDao, mappings)
            api(projects.core.model)
            api(libs.kotlinx.coroutines.core)
            // GrocyDatabase extends RoomDatabase
            api(libs.androidx.room3.runtime)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        wasmJsMain.dependencies {
            implementation(libs.androidx.sqlite.web)
            // org.w3c.dom.Worker for the database worker
            implementation(libs.kotlinx.browser)
            implementation(npm("grocy-sqlite-worker", file("web-worker")))
        }
    }
}
