package com.patrickzedler.grocy.core.data.log

import com.patrickzedler.grocy.core.data.BrowserSessionStorage
import com.patrickzedler.grocy.core.data.KeyValueStorage

/**
 * The web app's errors for the log screen, like logcat on Android: browsers do not let a page
 * read its console, so [install] records console errors, uncaught errors and unhandled promise
 * rejections. Kept per tab, so a reload after a crash still shows them; secrets are removed
 * before anything is stored.
 */
class BrowserErrorLog internal constructor(
    private val storage: KeyValueStorage,
    private val timestamp: () -> String,
) : LogReader {

    constructor() : this(BrowserSessionStorage, ::browserTimestamp)

    override val source = LogSource.BROWSER

    private val lines = ArrayDeque(storage.get(KEY)?.lines().orEmpty().filter { it.isNotEmpty() })

    /** Starts recording; call it once, as early as possible. */
    fun install() = captureBrowserErrors(::add)

    internal fun add(message: String) {
        val time = timestamp()
        redactSecrets(message).lines()
            .filter { it.isNotBlank() }
            .forEach { lines.addLast("$time E $it") }
        while (lines.size > MAX_LINES) lines.removeFirst()
        storage.set(KEY, lines.joinToString("\n"))
    }

    override suspend fun readErrors(): String = lines.joinToString("\n")

    internal companion object {
        const val KEY = "grocy.errorLog"

        // As many as the logcat screen on Android shows
        const val MAX_LINES = 300
    }
}

private val secrets = listOf(
    // Browsers send the API key as query parameter, so failing URLs contain it
    Regex("""(GROCY-API-KEY["']?\s*[=:]\s*["']?)[^&\s"',;}]+""", RegexOption.IGNORE_CASE),
    Regex("""(access_token["']?\s*[=:]\s*["']?)[^&\s"',;}]+""", RegexOption.IGNORE_CASE),
    Regex("""(ingress_session["']?\s*[=:]\s*["']?)[^&\s"',;}]+""", RegexOption.IGNORE_CASE),
    Regex("""(Bearer\s+)[^\s"',;}]+""", RegexOption.IGNORE_CASE),
)

/** [text] with API keys, tokens and sessions replaced, so a shared log reveals none of them. */
internal fun redactSecrets(text: String): String =
    secrets.fold(text) { result, secret -> secret.replace(result, "$1***") }

/** Local time like logcat: "10-17 14:03:59.042". */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """() => {
        const d = new Date();
        const pad = (n, length) => String(n).padStart(length, '0');
        return pad(d.getMonth() + 1, 2) + '-' + pad(d.getDate(), 2) + ' ' + pad(d.getHours(), 2)
            + ':' + pad(d.getMinutes(), 2) + ':' + pad(d.getSeconds(), 2) + '.'
            + pad(d.getMilliseconds(), 3);
    }""",
)
private external fun browserTimestamp(): String

/** Hands every console error, uncaught error and unhandled rejection to [onError] as text. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(onError) => {
        const describe = (value) => {
            if (value instanceof Error) return value.stack || String(value);
            if (typeof value === 'string') return value;
            try {
                return JSON.stringify(value) ?? String(value);
            } catch (e) {
                return String(value);
            }
        };
        // An error while recording must not be recorded again
        let recording = false;
        const record = (values) => {
            if (recording) return;
            recording = true;
            try {
                onError(values.map(describe).join(' '));
            } catch (e) {
                // The log is a help, it must never break the app
            } finally {
                recording = false;
            }
        };
        const consoleError = console.error;
        console.error = function (...values) {
            record(values);
            return consoleError.apply(this, values);
        };
        window.addEventListener('error', (event) => record([event.error ?? event.message]));
        window.addEventListener(
            'unhandledrejection',
            (event) => record(['Unhandled rejection:', event.reason]),
        );
    }""",
)
private external fun captureBrowserErrors(onError: (String) -> Unit)
