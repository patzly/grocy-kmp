plugins {
    alias(libs.plugins.grocy.kmp.library)
}

// Closed source (Google Play services), so only the Play app may depend on this module
kotlin {
    sourceSets {
        commonMain.dependencies {
            // The ML Kit engines implement BarcodeDecoder and TextRecognizer of the scanner
            api(projects.core.scanner)
        }
        androidMain.dependencies {
            implementation(libs.mlkit.barcode.scanning)
            implementation(libs.mlkit.text.recognition)
        }
    }
}
