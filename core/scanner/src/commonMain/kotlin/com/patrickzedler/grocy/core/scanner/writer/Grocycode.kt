package com.patrickzedler.grocy.core.scanner.writer

/**
 * Grocy's grocycodes, its own codes of objects: "grcy", the type and the id, separated by
 * colons; stock entries add their stock id ("grcy:p:12:63f1a2b3c4d5e").
 */
object Grocycode {

    fun product(productId: Int): String = "$MAGIC:p:$productId"

    fun stockEntry(productId: Int, stockId: String): String = "$MAGIC:p:$productId:$stockId"

    /** The symbol of [code]: Data Matrix for Grocy's 2D codes, Code 128 for 1D ones. */
    fun matrix(code: String, twoDimensional: Boolean): BarcodeMatrix =
        if (twoDimensional) DataMatrixWriter.write(code) else Code128Writer.write(code)

    private const val MAGIC = "grcy"
}
