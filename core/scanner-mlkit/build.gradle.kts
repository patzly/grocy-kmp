plugins {
    alias(libs.plugins.grocy.kmp.library)
}

// Closed source (Google Play services), so only the Play app may depend on this module
kotlin {
    sourceSets {
        commonMain.dependencies {
            // MlKitBarcodeDecoder implements BarcodeDecoder and is handed to the scanner as such
            api(projects.core.scanner)
        }
        androidMain.dependencies {
            implementation(libs.mlkit.barcode.scanning)
        }
    }
}
