package com.patrickzedler.grocy.core.model

/** Barcode decoding library of the scanner. Stored by name, so entries must not be renamed. */
enum class ScannerEngine {
    /** Open source, available everywhere. */
    ZXING,

    /** Google's ML Kit, only in the Play app and only when unlocked. */
    ML_KIT,
}
