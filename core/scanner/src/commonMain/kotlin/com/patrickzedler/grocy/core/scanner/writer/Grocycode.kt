package com.patrickzedler.grocy.core.scanner.writer

/**
 * Grocy's grocycodes, its own codes of objects: "grcy", the type and the id, separated by
 * colons; stock entries add their stock id ("grcy:p:12:63f1a2b3c4d5e").
 */
object Grocycode {

    fun product(productId: Int): String = "$MAGIC:p:$productId"

    fun stockEntry(productId: Int, stockId: String): String = "$MAGIC:p:$productId:$stockId"

    fun chore(choreId: Int): String = "$MAGIC:c:$choreId"

    fun battery(batteryId: Int): String = "$MAGIC:b:$batteryId"

    /** The chore of a scanned grocycode, null for every other code. */
    fun choreIdOf(code: String): Int? = idOf(code, type = "c")

    /** The battery of a scanned grocycode, null for every other code. */
    fun batteryIdOf(code: String): Int? = idOf(code, type = "b")

    private fun idOf(code: String, type: String): Int? {
        val parts = code.trim().split(':')
        if (parts.size < OBJECT_CODE_PARTS || parts[0] != MAGIC || parts[1] != type) return null
        return parts[2].toIntOrNull()
    }

    /** The symbol of [code]: Data Matrix for Grocy's 2D codes, Code 128 for 1D ones. */
    fun matrix(code: String, twoDimensional: Boolean): BarcodeMatrix =
        if (twoDimensional) DataMatrixWriter.write(code) else Code128Writer.write(code)

    private const val MAGIC = "grcy"
    private const val OBJECT_CODE_PARTS = 3
}
