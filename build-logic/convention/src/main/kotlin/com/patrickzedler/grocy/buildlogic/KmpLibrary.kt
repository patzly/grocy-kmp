package com.patrickzedler.grocy.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Android and Wasm targets plus test setup shared by all KMP library modules.
 *
 * commonTest always runs on the JVM as Android host test. With [runWasmTests] it also runs on
 * Wasm in Node.js, which needs no browser.
 */
internal fun Project.configureKmpLibrary(runWasmTests: Boolean) {
    pluginManager.apply(libs.pluginId("kotlin-multiplatform"))
    pluginManager.apply(libs.pluginId("android-kmp-library"))

    val moduleNamespace = derivedNamespace
    val compileSdkLevel = libs.intVersion("android-compileSdk")
    val compileSdkMinorLevel = libs.intVersion("android-compileSdkMinor")
    val minSdkLevel = libs.intVersion("android-minSdk")

    extensions.configure<KotlinMultiplatformExtension> {
        compilerOptions {
            // Platform types such as CameraFrame (ImageProxy on Android, ImageData on the web)
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }

        targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
            namespace = moduleNamespace
            compileSdk {
                version = release(compileSdkLevel) {
                    minorApiLevel = compileSdkMinorLevel
                }
            }
            minSdk = minSdkLevel
            compilerOptions {
                jvmTarget.set(GrocyJvmTarget)
            }
            withHostTest {}
        }

        @OptIn(ExperimentalWasmDsl::class)
        wasmJs {
            browser {
                // Browser tests need an installed browser, Node runs the same tests without
                testTask { enabled = false }
            }
            if (runWasmTests) {
                nodejs()
            }
        }

        sourceSets.commonTest.dependencies {
            implementation(libs.library("kotlin-test"))
            implementation(libs.library("kotlinx-coroutines-test"))
            implementation(libs.library("turbine"))
        }
        // setMain, test dispatchers and testTimeSource are the standard tools in tests
        sourceSets.matching { it.name.endsWith("Test") }.configureEach {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
        }
    }
}
