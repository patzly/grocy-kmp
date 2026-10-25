package com.patrickzedler.grocy.feature.settings.impl.general

internal actual fun languageName(tag: String): String =
    displayName(tag).replaceFirstChar { it.uppercaseChar() }

/** Browsers without Intl.DisplayNames or with an unknown tag show the tag itself. */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """(tag) => {
        try {
            return new Intl.DisplayNames([tag], { type: 'language' }).of(tag) || tag;
        } catch (e) {
            return tag;
        }
    }""",
)
private external fun displayName(tag: String): String

internal actual fun languageNameComparator(): Comparator<String> =
    Comparator { a, b -> compareNames(a, b) }

/** The browser's collation of its language (navigator.language, the app's). */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("(a, b) => a.localeCompare(b)")
private external fun compareNames(a: String, b: String): Int
