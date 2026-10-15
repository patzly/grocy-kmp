package com.patrickzedler.grocy.core.scanner

/** Barcode symbologies the scanner understands, the same set for every engine. */
enum class BarcodeFormat(
    /** Strong error correction: one read is reliable. Linear codes need a confirmation. */
    val isTwoDimensional: Boolean = false,
) {
    AZTEC(isTwoDimensional = true),
    CODABAR,
    CODE_39,
    CODE_93,
    CODE_128,
    DATA_MATRIX(isTwoDimensional = true),
    EAN_8,
    EAN_13,
    ITF,
    PDF_417(isTwoDimensional = true),
    QR_CODE(isTwoDimensional = true),
    UPC_A,
    UPC_E,
    ;

    companion object {
        /** Codes on products and Grocy's own grocycodes (QR code and Data Matrix). */
        val Products: Set<BarcodeFormat> = setOf(
            EAN_13, EAN_8, UPC_A, UPC_E, CODE_128, CODE_39, CODE_93, ITF, CODABAR,
            QR_CODE, DATA_MATRIX,
        )

        /** Only QR codes, e.g. the login code of Grocy's API key page. */
        val QrCode: Set<BarcodeFormat> = setOf(QR_CODE)
    }
}

data class Barcode(
    val value: String,
    val format: BarcodeFormat,
)
