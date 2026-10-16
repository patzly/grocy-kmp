package com.patrickzedler.grocy.buildlogic

import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin

/**
 * Node.js and Binaryen come from the repositories declared in settings.gradle.kts. Without a
 * download URL Kotlin adds no project repositories of its own, which settings would ignore.
 */
@OptIn(ExperimentalWasmDsl::class)
internal fun Project.useDeclaredWasmToolingRepositories() {
    plugins.withType<WasmNodeJsPlugin> {
        the<WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
    }
    plugins.withType<BinaryenPlugin> {
        the<BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
    }
}
