import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
        import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
        import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

        plugins {
            alias(libs.plugins.android.application) apply false
            alias(libs.plugins.android.kmp.library) apply false
            alias(libs.plugins.kotlin.multiplatform) apply false
            alias(libs.plugins.kotlin.serialization) apply false
            alias(libs.plugins.compose.multiplatform) apply false
            alias(libs.plugins.compose.compiler) apply false
        }

// Locally the npm lock file updates itself, in CI an outdated lock file fails the build
val isCi = providers.environmentVariable("CI").isPresent
plugins.withType<WasmYarnPlugin> {
    the<WasmYarnRootEnvSpec>().apply {
        yarnLockAutoReplace.set(!isCi)
        yarnLockMismatchReport.set(if (isCi) YarnLockMismatchReport.FAIL else YarnLockMismatchReport.WARNING)
    }
}
