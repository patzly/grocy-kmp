plugins {
    alias(libs.plugins.grocy.kmp.library)
}

// Fakes for tests of other modules, only ever added as a commonTest dependency
kotlin {
    sourceSets {
        commonMain.dependencies {
            // The fakes implement interfaces from core:data and expose its types
            api(projects.core.data)
            api(libs.ktor.client.mock)
        }
        androidMain.dependencies {
            // RepositoryTestEnvironment runs on the TestDispatcher of the test
            api(libs.kotlinx.coroutines.test)
        }
    }
}
