plugins {
    `kotlin-dsl`
}

group = "com.patrickzedler.grocy.buildlogic"

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.room3.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = libs.plugins.grocy.kmp.library.get().pluginId
            implementationClass = "com.patrickzedler.grocy.buildlogic.KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = libs.plugins.grocy.kmp.compose.get().pluginId
            implementationClass = "com.patrickzedler.grocy.buildlogic.KmpComposeConventionPlugin"
        }
        register("androidApplication") {
            id = libs.plugins.grocy.android.application.get().pluginId
            implementationClass = "com.patrickzedler.grocy.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("room") {
            id = libs.plugins.grocy.room.get().pluginId
            implementationClass = "com.patrickzedler.grocy.buildlogic.RoomConventionPlugin"
        }
        register("sqliteHostTests") {
            id = libs.plugins.grocy.sqlite.host.tests.get().pluginId
            implementationClass = "com.patrickzedler.grocy.buildlogic.SqliteHostTestsConventionPlugin"
        }
        register("webApplication") {
            id = libs.plugins.grocy.web.application.get().pluginId
            implementationClass = "com.patrickzedler.grocy.buildlogic.WebApplicationConventionPlugin"
        }
    }
}