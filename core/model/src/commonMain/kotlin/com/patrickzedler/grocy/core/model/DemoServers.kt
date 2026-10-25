package com.patrickzedler.grocy.core.model

/** A public demo of Grocy, set up in one language. */
data class DemoServer(
    /** BCP 47 tag of the demo's language, e.g. "pt-BR". */
    val languageTag: String,
    val url: String,
)

/**
 * Grocy's public demos in the order of the language menu the demos show themselves
 * (https://en.demo.grocy.info), one per language Grocy is translated into.
 */
val DemoServers: List<DemoServer> = listOf(
    demo("bg", "bg"),
    demo("ca", "ca"),
    demo("cs", "cs"),
    demo("da", "da"),
    demo("de", "de"),
    demo("el", "el"),
    demo("en", "en"),
    demo("es", "es"),
    demo("et", "et"),
    demo("fi", "fi"),
    demo("fr", "fr"),
    demo("he", "he-il"),
    demo("hu", "hu"),
    demo("it", "it"),
    demo("ja", "ja"),
    demo("ko", "ko"),
    demo("lt", "lt"),
    demo("nl", "nl"),
    // Norwegian Bokmål, which the app calls "nb"
    demo("nb", "no"),
    demo("pl", "pl"),
    demo("pt-BR", "pt-br"),
    demo("pt-PT", "pt-pt"),
    demo("ro", "ro"),
    demo("ru", "ru"),
    demo("sk", "sk"),
    demo("sl", "sl"),
    demo("sv", "sv"),
    demo("ta", "ta"),
    demo("tr", "tr"),
    demo("uk", "uk"),
    demo("zh-CN", "zh-cn"),
    demo("zh-TW", "zh-tw"),
)

private fun demo(languageTag: String, subdomain: String) =
    DemoServer(languageTag, "https://$subdomain.demo.grocy.info")

/**
 * The demo in the language of [tag] (a BCP 47 tag such as "de-CH"), null if there is none. A
 * regional tag without a demo of its own uses its base language's.
 */
fun demoServerFor(tag: String?, servers: List<DemoServer> = DemoServers): DemoServer? {
    if (tag.isNullOrBlank()) return null
    val normalized = tag.replace('_', '-')
    val base = normalized.substringBefore('-').let { LanguageAliases[it.lowercase()] ?: it }
    return servers.firstOrNull { it.languageTag.equals(normalized, ignoreCase = true) }
        ?: servers.firstOrNull { it.languageTag.equals(base, ignoreCase = true) }
}

/** The demo [url] belongs to, null for any other server. */
fun demoServerOf(url: String, servers: List<DemoServer> = DemoServers): DemoServer? =
    servers.firstOrNull { it.url.equals(url.trimEnd('/'), ignoreCase = true) }

/** Old or other codes of the same language: Android's "iw" for Hebrew, "no" for Bokmål. */
private val LanguageAliases = mapOf("iw" to "he", "no" to "nb")
