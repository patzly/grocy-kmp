plugins {
    alias(libs.plugins.grocy.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    defaultConfig {
        minSdk {
            version = release(libs.versions.android.wear.minSdk.get().toInt())
        }
        // Must differ from the phone app, both share one Play Store listing
        versionCode = providers.gradleProperty("grocy.versionCode").get().toInt() + 1_000_000
    }
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.core.database)
    implementation(projects.core.network)
    implementation(projects.core.resources)
    implementation(projects.core.wear)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.wear.compose.foundation)
    implementation(libs.androidx.wear.compose.material3)
    implementation(libs.androidx.wear.compose.navigation)
    implementation(libs.androidx.wear.input)
    implementation(libs.jetbrains.lifecycle.runtime.compose)
    implementation(libs.jetbrains.lifecycle.viewmodel.compose)
    implementation(libs.play.services.wearable)
}
