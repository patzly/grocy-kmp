package com.patrickzedler.grocy.core.scanner

/**
 * Format names of zxing-wasm. Inputs use the symbology names, outputs can name a variant
 * (e.g. "QRCodeModel2", "ITF14", "ISBN"), so reading maps variants to their symbology.
 */
internal object ZxingWasmFormats {

    fun nameOf(format: BarcodeFormat): String = when (format) {
        BarcodeFormat.AZTEC -> "Aztec"
        BarcodeFormat.CODABAR -> "Codabar"
        BarcodeFormat.CODE_39 -> "Code39"
        BarcodeFormat.CODE_93 -> "Code93"
        BarcodeFormat.CODE_128 -> "Code128"
        BarcodeFormat.DATA_MATRIX -> "DataMatrix"
        BarcodeFormat.EAN_8 -> "EAN8"
        BarcodeFormat.EAN_13 -> "EAN13"
        BarcodeFormat.ITF -> "ITF"
        BarcodeFormat.PDF_417 -> "PDF417"
        BarcodeFormat.QR_CODE -> "QRCode"
        BarcodeFormat.UPC_A -> "UPCA"
        BarcodeFormat.UPC_E -> "UPCE"
    }

    fun formatOf(name: String): BarcodeFormat? = when (name) {
        "Aztec", "AztecCode", "AztecRune" -> BarcodeFormat.AZTEC
        "Codabar" -> BarcodeFormat.CODABAR
        "Code39", "Code39Std", "Code39Ext" -> BarcodeFormat.CODE_39
        "Code93" -> BarcodeFormat.CODE_93
        "Code128" -> BarcodeFormat.CODE_128
        "DataMatrix" -> BarcodeFormat.DATA_MATRIX
        "EAN8" -> BarcodeFormat.EAN_8
        // ISBN is an EAN-13 starting with 978 or 979
        "EAN13", "ISBN" -> BarcodeFormat.EAN_13
        "ITF", "ITF14" -> BarcodeFormat.ITF
        "PDF417", "CompactPDF417" -> BarcodeFormat.PDF_417
        "QRCode", "QRCodeModel1", "QRCodeModel2" -> BarcodeFormat.QR_CODE
        "UPCA" -> BarcodeFormat.UPC_A
        "UPCE" -> BarcodeFormat.UPC_E
        else -> null
    }
}
