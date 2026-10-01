package com.patrickzedler.grocy.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Kotlin Multiplatform library for Android and Web (Wasm), without Compose.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("kotlin-multiplatform"))
        pluginManager.apply(libs.pluginId("android-kmp-library"))

        val moduleNamespace = derivedNamespace
        val compileSdkLevel = libs.intVersion("android-compileSdk")
        val minSdkLevel = libs.intVersion("android-minSdk")

        extensions.configure<KotlinMultiplatformExtension> {
            targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                namespace = moduleNamespace
                compileSdk = compileSdkLevel
                minSdk = minSdkLevel
                compilerOptions {
                    jvmTarget.set(GrocyJvmTarget)
                }
            }

            @OptIn(ExperimentalWasmDsl::class)
            wasmJs {
                browser()
            }
        }
    }
}