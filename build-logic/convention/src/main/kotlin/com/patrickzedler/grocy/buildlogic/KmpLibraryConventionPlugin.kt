package com.patrickzedler.grocy.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Kotlin Multiplatform library for Android and Web (Wasm), without Compose.
 *
 * commonTest runs twice: on the JVM (Android host test) and on Wasm in Node.js.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        configureKmpLibrary(runWasmTests = true)
    }
}
