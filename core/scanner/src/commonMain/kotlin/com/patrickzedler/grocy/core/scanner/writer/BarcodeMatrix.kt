package com.patrickzedler.grocy.core.scanner.writer

/** The modules of a written code, row by row; true is dark. One row for linear codes. */
class BarcodeMatrix(val width: Int, val height: Int) {

    private val modules = BooleanArray(width * height)

    operator fun get(x: Int, y: Int): Boolean = modules[y * width + x]

    internal operator fun set(x: Int, y: Int, dark: Boolean) {
        modules[y * width + x] = dark
    }

    override fun toString(): String = buildString {
        for (y in 0 until height) {
            for (x in 0 until width) append(if (this@BarcodeMatrix[x, y]) '#' else '.')
            appendLine()
        }
    }
}
