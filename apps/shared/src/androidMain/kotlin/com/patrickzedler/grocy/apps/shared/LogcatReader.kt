package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.data.log.LogReader
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Android lets an app read only its own log lines, so nothing of other apps appears. */
internal class LogcatReader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LogReader {

    override suspend fun readErrors(): String = withContext(ioDispatcher) {
        try {
            // Errors only, the newest lines; -t implies dumping instead of following
            val process = ProcessBuilder("logcat", "*:E", "-t", MAX_LINES.toString())
                .redirectErrorStream(true)
                .start()
            try {
                process.inputStream.bufferedReader().use { it.readText() }
            } finally {
                process.destroy()
            }
        } catch (_: IOException) {
            ""
        }
    }

    private companion object {
        const val MAX_LINES = 300
    }
}
