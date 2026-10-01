package com.patrickzedler.grocy.buildlogic

import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * Android app on AGP 9 with built-in Kotlin (no org.jetbrains.kotlin.android plugin).
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-application"))
        pluginManager.apply(libs.pluginId("compose-compiler"))

        val moduleNamespace = derivedNamespace
        val compileSdkLevel = libs.intVersion("android-compileSdk")
        val minSdkLevel = libs.intVersion("android-minSdk")
        val targetSdkLevel = libs.intVersion("android-targetSdk")
        val appVersionName = providers.gradleProperty("grocy.versionName").orNull
            ?: error("Missing grocy.versionName in the root gradle.properties")
        val appVersionCode = providers.gradleProperty("grocy.versionCode").orNull?.toInt()
            ?: error("Missing grocy.versionCode in the root gradle.properties")
        val nightlyVersionSuffix = providers.gradleProperty("grocy.nightlyVersionSuffix")
            .getOrElse("-nightly")

        extensions.configure<ApplicationExtension> {
            namespace = moduleNamespace
            compileSdk {
                version = release(compileSdkLevel)
            }

            defaultConfig {
                // Identical for all Android apps: phone and watch must share it for the Wear Data Layer
                applicationId = "com.patrickzedler.grocy"
                versionCode = appVersionCode
                versionName = appVersionName
                minSdk {
                    version = release(minSdkLevel)
                }
                targetSdk {
                    version = release(targetSdkLevel)
                }
            }

            // Without the environment variables the builds stay unsigned, never debug-signed
            val releaseSigning = signingConfigFromEnv(target, name = "release", prefix = "RELEASE")
            val nightlySigning = signingConfigFromEnv(target, name = "nightly", prefix = "NIGHTLY")

            buildTypes {
                getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
                    signingConfig = releaseSigning
                    // The commit hash in the APK would be one more input to keep identical
                    vcsInfo {
                        include = false
                    }
                }
                create("nightly") {
                    initWith(getByName("release"))
                    applicationIdSuffix = ".nightly"
                    versionNameSuffix = nightlyVersionSuffix
                    signingConfig = nightlySigning
                    matchingFallbacks += "release"
                }
            }

            // Encrypted Google blob, rejected by F-Droid and IzzyOnDroid; still allowed in Play bundles
            dependenciesInfo {
                includeInApk = false
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }

            buildFeatures {
                compose = true
            }
        }

        tasks.withType<KotlinJvmCompile>().configureEach {
            compilerOptions.jvmTarget.set(GrocyJvmTarget)
        }
    }
}

private fun ApplicationExtension.signingConfigFromEnv(
    project: Project,
    name: String,
    prefix: String,
): ApkSigningConfig? {
    val env = project.providers
    val keystorePath = env.environmentVariable("${prefix}_KEYSTORE_PATH").orNull ?: return null
    return signingConfigs.create(name).apply {
        storeFile = project.file(keystorePath)
        storePassword = env.environmentVariable("${prefix}_KEYSTORE_PASSWORD").get()
        keyAlias = env.environmentVariable("${prefix}_KEY_ALIAS").get()
        keyPassword = env.environmentVariable("${prefix}_KEY_PASSWORD").get()
    }
}