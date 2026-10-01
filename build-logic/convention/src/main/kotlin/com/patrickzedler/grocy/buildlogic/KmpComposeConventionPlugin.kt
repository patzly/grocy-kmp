package com.patrickzedler.grocy.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Kotlin Multiplatform library with Compose Multiplatform UI.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(KmpLibraryConventionPlugin::class.java)
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

        // Compose previews in Android Studio, kept out of the compile classpath
        val uiTooling = libs.library("compose-ui-tooling")
        configurations.matching { it.name == "androidRuntimeClasspath" }.configureEach {
            target.dependencies.addProvider(name, uiTooling)
        }
    }
}