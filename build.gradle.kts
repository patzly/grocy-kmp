import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnLockMismatchReport
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.nodejs.WasmNodeJsPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
}

// Node.js, Yarn and Binaryen come from the repositories declared in settings.gradle.kts; without
// a download URL Kotlin adds no project repositories of its own, which settings would ignore.
// Subprojects get the same through the convention plugins (useDeclaredWasmToolingRepositories).
plugins.withType<WasmNodeJsPlugin> {
    the<WasmNodeJsEnvSpec>().downloadBaseUrl.set(null as String?)
}
@OptIn(ExperimentalWasmDsl::class)
plugins.withType<BinaryenPlugin> {
    the<BinaryenEnvSpec>().downloadBaseUrl.set(null as String?)
}

// Locally the npm lock file updates itself, in CI an outdated lock file fails the build, so npm
// dependencies only change through a reviewed commit
val isCi = providers.environmentVariable("CI").isPresent
plugins.withType<WasmYarnPlugin> {
    the<WasmYarnRootEnvSpec>().apply {
        downloadBaseUrl.set(null as String?)
        yarnLockAutoReplace.set(!isCi)
        yarnLockMismatchReport.set(
            if (isCi) YarnLockMismatchReport.FAIL else YarnLockMismatchReport.WARNING,
        )
    }
}
