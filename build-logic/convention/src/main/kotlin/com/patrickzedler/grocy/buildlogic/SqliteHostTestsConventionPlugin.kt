package com.patrickzedler.grocy.buildlogic

import javax.inject.Inject
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.newInstance
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.process.CommandLineArgumentProvider

/**
 * Lets JVM host tests open real SQLite databases with `BundledSQLiteDriver`: its Android build
 * only ships Android libraries, so the tests load the desktop library of the JVM build through
 * the driver's loader properties.
 */
class SqliteHostTestsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val natives = configurations.create("bundledSqliteNatives") {
            isTransitive = false
            isCanBeConsumed = false
        }
        dependencies.add(natives.name, libs.library("androidx-sqlite-bundled-jvm"))

        // Injected instead of Project.zipTree, which the configuration cache cannot store
        val archives = objects.newInstance<ArchiveServices>().archives
        val extract = tasks.register<Sync>("extractBundledSqliteNatives") {
            from(natives.elements.map { files -> files.map { archives.zipTree(it) } }) {
                include("natives/$hostPlatform/**")
                eachFile { path = name }
                includeEmptyDirs = false
            }
            into(layout.buildDirectory.dir("sqlite-natives"))
        }
        val directory = layout.buildDirectory.dir("sqlite-natives").map { it.asFile.absolutePath }
        val libraryName = hostLibraryName

        tasks.withType<Test>().configureEach {
            dependsOn(extract)
            jvmArgumentProviders.add(
                CommandLineArgumentProvider {
                    listOf(
                        "-Dandroidx.sqlite.driver.bundled.path=${directory.get()}",
                        "-Dandroidx.sqlite.driver.bundled.name=$libraryName",
                    )
                },
            )
        }
    }
}

internal abstract class ArchiveServices @Inject constructor(val archives: ArchiveOperations)

private val osName = System.getProperty("os.name").lowercase()
private val isArm = System.getProperty("os.arch").let { it == "aarch64" || it == "arm64" }

/** Directory of the desktop library in sqlite-bundled-jvm. */
private val hostPlatform: String
    get() = when {
        osName.startsWith("mac") -> "osx_arm64"
        osName.startsWith("windows") -> "windows_x64"
        isArm -> "linux_arm64"
        else -> "linux_x64"
    }

private val hostLibraryName: String
    get() = when {
        osName.startsWith("mac") -> "libsqliteJni.dylib"
        osName.startsWith("windows") -> "sqliteJni.dll"
        else -> "libsqliteJni.so"
    }
