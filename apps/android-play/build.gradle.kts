plugins {
    alias(libs.plugins.grocy.android.application)
}

dependencies {
    implementation(projects.apps.shared)
    // Closed source, which is why only this app may use it
    implementation(projects.core.scannerMlkit)
    implementation(projects.core.updatePlay)
    implementation(projects.core.wear)
    implementation(libs.play.services.wearable)
}
