package com.patrickzedler.grocy.buildlogic

import androidx.room3.gradle.RoomExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Room 3 in a KMP library: KSP generates the database code for Android and Wasm, the schema of
 * every version is exported to `schemas/`, so migrations can be checked against it.
 */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("ksp"))
        pluginManager.apply(libs.pluginId("room3"))
        pluginManager.apply(SqliteHostTestsConventionPlugin::class.java)

        extensions.configure<RoomExtension> {
            schemaDirectory(layout.projectDirectory.dir("schemas").asFile.path)
        }

        val compiler = libs.library("androidx-room3-compiler")
        dependencies {
            // One configuration per target, the generic ksp(...) is deprecated in KMP
            add("kspAndroid", compiler)
            add("kspWasmJs", compiler)
        }
    }
}
