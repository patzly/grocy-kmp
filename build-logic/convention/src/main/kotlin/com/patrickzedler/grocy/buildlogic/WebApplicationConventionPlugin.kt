package com.patrickzedler.grocy.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Browser application built with Compose Multiplatform for Kotlin/Wasm.
 */
class WebApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-multiplatform"))
        pluginManager.apply(libs.pluginId("compose-multiplatform"))
        pluginManager.apply(libs.pluginId("compose-compiler"))

        extensions.configure<KotlinMultiplatformExtension> {
            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                outputModuleName.set("grocy")
                browser {
                    commonWebpackConfig {
                        outputFileName = "grocy.js"
                    }
                }
                binaries.executable()
            }

            sourceSets.wasmJsMain.dependencies {
                // ComposeViewport, the browser entry point of every web app
                implementation(libs.library("compose-runtime"))
                implementation(libs.library("compose-ui"))
            }
        }
    }
}
