plugins {
    alias(libs.plugins.grocy.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.design)
            implementation(projects.core.resources)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
        }
        androidMain.dependencies {
            // CameraFrame wraps ImageProxy, which other decoder modules need to read
            api(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.compose)
            implementation(libs.androidx.activity.compose)
            implementation(libs.zxing.core)
        }
        wasmJsMain.dependencies {
            implementation(npm("zxing-wasm", libs.versions.npm.zxing.wasm.get()))
            implementation(libs.kotlinx.browser)
        }
    }
}
