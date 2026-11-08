plugins {
    alias(libs.plugins.grocy.kmp.library)
}

// Closed source (Google Play services), so only the Play app may depend on this module
kotlin {
    android {
        optimization {
            // The app's R8 needs them to keep what ML Kit creates by reflection
            consumerKeepRules.file("consumer-rules.pro")
        }
    }
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
