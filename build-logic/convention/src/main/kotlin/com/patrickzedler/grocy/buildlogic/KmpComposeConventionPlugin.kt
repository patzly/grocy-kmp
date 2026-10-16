package com.patrickzedler.grocy.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Kotlin Multiplatform library with Compose Multiplatform UI.
 *
 * commonTest runs on the JVM only: Compose on Wasm needs the Skiko runtime of a browser.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        configureKmpLibrary(runWasmTests = false)
        pluginManager.apply(libs.pluginId("compose-multiplatform"))
        pluginManager.apply(libs.pluginId("compose-compiler"))

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.commonMain.dependencies {
                implementation(libs.library("compose-runtime"))
                implementation(libs.library("compose-foundation"))
                implementation(libs.library("compose-ui"))
                implementation(libs.library("compose-ui-tooling-preview"))
            }
        }

        // These modules run no Wasm tests, so their test binaries are neither compiled nor linked.
        // The package.json tasks keep running: their files are part of the shared yarn workspace,
        // and stale ones would make the local yarn.lock differ from the one CI computes.
        tasks.matching {
            it.name.contains("WasmJs", ignoreCase = true) &&
                it.name.contains("Test") &&
                !it.name.contains("PackageJson")
        }.configureEach { enabled = false }

        // Compose previews in Android Studio, kept out of the compile classpath
        val uiTooling = libs.library("compose-ui-tooling")
        configurations.matching { it.name == "androidRuntimeClasspath" }.configureEach {
            target.dependencies.addProvider(name, uiTooling)
        }
    }
}
