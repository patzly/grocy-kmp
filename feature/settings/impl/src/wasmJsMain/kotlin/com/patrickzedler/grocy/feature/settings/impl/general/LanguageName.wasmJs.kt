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
