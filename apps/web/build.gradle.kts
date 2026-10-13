plugins {
    alias(libs.plugins.grocy.web.application)
}

val generateBuildInfo = tasks.register("generateBuildInfo") {
    group = "build"
    description = "Generates BuildInfo.kt with the app version for the web app."
    val versionName = providers.gradleProperty("grocy.versionName")
    val outputDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")
    inputs.property("versionName", versionName)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/patrickzedler/grocy/web/BuildInfo.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.patrickzedler.grocy.web
            |
            |internal const val APP_VERSION_NAME = "${versionName.get()}"
            |
            """.trimMargin(),
        )
    }
}

kotlin {
    sourceSets {
        wasmJsMain {
            kotlin.srcDir(generateBuildInfo)
            dependencies {
                implementation(projects.apps.shared)
            }
        }
    }
}
