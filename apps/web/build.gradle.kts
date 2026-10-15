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

// zxing-wasm downloads its decoder from a CDN by default. The app serves it itself instead:
// no third party sees who scans, and the version is pinned by yarn.lock.
val copyZxingWasm = tasks.register<Sync>("copyZxingWasm") {
    group = "build"
    description = "Copies the zxing-wasm decoder next to index.html."
    dependsOn(":kotlinWasmNpmInstall")
    from(
        rootProject.layout.buildDirectory
            .file("wasm/node_modules/zxing-wasm/dist/reader/zxing_reader.wasm"),
    )
    into(layout.buildDirectory.dir("generated/zxingWasm"))
}

kotlin {
    sourceSets {
        wasmJsMain {
            kotlin.srcDir(generateBuildInfo)
            resources.srcDir(copyZxingWasm)
            dependencies {
                implementation(projects.apps.shared)
                implementation(projects.core.design)
            }
        }
    }
}
