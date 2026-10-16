package com.patrickzedler.grocy.core.data.log

/** Recent error log of the app, for bug reports; only Android has one (logcat). */
fun interface LogReader {

    /** The newest errors as text, oldest first; empty if there are none or reading failed. */
    suspend fun readErrors(): String
}
