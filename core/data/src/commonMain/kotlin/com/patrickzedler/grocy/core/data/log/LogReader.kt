package com.patrickzedler.grocy.core.data.log

/** Where the lines of a [LogReader] come from, which names the log screen. */
enum class LogSource {
    /** Android's log of the app. */
    LOGCAT,

    /** Errors the web app recorded itself, browsers keep their console to themselves. */
    BROWSER,
}

/** Recent error log of the app, for bug reports. */
interface LogReader {

    val source: LogSource

    /** The newest errors as text, oldest first; empty if there are none or reading failed. */
    suspend fun readErrors(): String
}
