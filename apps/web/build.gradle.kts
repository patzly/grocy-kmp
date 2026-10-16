plugins {
    alias(libs.plugins.grocy.web.application)
}

// CI sets both for deployments, e.g. the commit SHA and "-nightly+<short SHA>". Without a build
// id ("dev", DEV_BUILD_ID in core:data) the app neither caches itself nor looks for updates.
val webBuildId = providers.gradleProperty("grocy.webBuildId").orElse("dev")
val webVersionName = providers.gradleProperty("grocy.versionName")
    .zip(providers.gradleProperty("grocy.webVersionSuffix").orElse("")) { name, suffix ->
        name + suffix
    }

val generateBuildInfo = tasks.register("generateBuildInfo") {
    group = "build"
    description = "Generates BuildInfo.kt with the version and build id of the web app."
    val versionName = webVersionName
    val buildId = webBuildId
    val outputDir = layout.buildDirectory.dir("generated/buildInfo/kotlin")
    inputs.property("versionName", versionName)
    inputs.property("buildId", buildId)
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
            |internal const val APP_BUILD_ID = "${buildId.get()}"
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

// precache.json describes the deployment: its build id and version for the update check, and
// every file the service worker (sw.js) caches, so a version is always complete
tasks.named<Sync>("wasmJsBrowserDistribution") {
    val buildId = webBuildId
    val versionName = webVersionName
    val outputDir = destinationDir
    inputs.property("buildId", buildId)
    inputs.property("versionName", versionName)
    doLast {
        val files = outputDir.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(outputDir).invariantSeparatorsPath }
            .filter { !it.endsWith(".map") && it != "sw.js" && it != "precache.json" }
            .sorted()
            .toList()
        fun String.json() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        outputDir.resolve("precache.json").writeText(
            buildString {
                append("{\"buildId\":").append(buildId.get().json())
                append(",\"versionName\":").append(versionName.get().json())
                append(",\"files\":[")
                append(files.joinToString(",") { it.json() })
                append("]}\n")
            },
        )
    }
}
