package com.patrickzedler.grocy.apps.shared

import com.patrickzedler.grocy.core.model.AppLanguages
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The language lists of the app, Android's system settings and the translations must match. */
class LocalesConfigTest {

    // Gradle runs host tests in the module directory
    private val resources = File("../../core/resources/src/commonMain/composeResources")

    /** Translations that exist twice: Android still reports the old ISO code. */
    private val legacyCopies = mapOf("he" to "iw")

    @Test
    fun localesConfigListsEveryAppLanguage() {
        val config = File("src/androidMain/res/xml/locales_config.xml").readText()
        val configTags = Regex("""<locale android:name="([^"]+)"""").findAll(config)
            .map { it.groupValues[1] }
            .toList()

        assertEquals(AppLanguages.sorted(), configTags.sorted())
    }

    @Test
    fun phoneAppsDeclareTheLocalesConfig() {
        // The manifest merger rejects android:localeConfig from libraries like this module
        listOf("android-foss", "android-play").forEach { app ->
            val manifest = File("../$app/src/main/AndroidManifest.xml").readText()

            assertTrue(
                """android:localeConfig="@xml/locales_config"""" in manifest,
                "$app must declare the locales config for the system language settings",
            )
        }
    }

    @Test
    fun everyAppLanguageAndNothingElseHasATranslation() {
        // English is the base in values/, every other language needs values-<qualifier>/
        val expected = AppLanguages.drop(1).map(::qualifierOf) + legacyCopies.values

        assertEquals(expected.sorted(), translationDirectories().sorted())
    }

    @Test
    fun legacyCopiesEqualTheirTranslation() {
        legacyCopies.forEach { (tag, legacy) ->
            assertEquals(
                File(resources, "values-$tag/strings.xml").readText(),
                File(resources, "values-$legacy/strings.xml").readText(),
                "values-$legacy must be a copy of values-$tag",
            )
        }
    }

    @Test
    fun translationsKeepTheTextsAndPlaceholdersOfEnglish() {
        val english = File(resources, "values/strings.xml").strings()
        val translatable = english.filterKeys { it !in untranslatable() }
        translationDirectories().forEach { qualifier ->
            val translation = File(resources, "values-$qualifier/strings.xml").strings()
            // Missing texts fall back to English, unknown ones are leftovers
            assertEquals(emptySet(), translation.keys - translatable.keys, "Unknown in $qualifier")
            translation.forEach { (name, text) ->
                assertEquals(
                    placeholders(translatable.getValue(name)),
                    placeholders(text),
                    "Placeholders of $name in $qualifier",
                )
            }
        }
    }

    /** Qualifiers of the translated values directories, e.g. "de" or "pt-rBR". */
    private fun translationDirectories(): List<String> =
        resources.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values-") }
            .map { it.name.removePrefix("values-") }

    /** "pt-BR" becomes "pt-rBR", the region qualifier of resource directories. */
    private fun qualifierOf(tag: String): String =
        tag.split('-').let { parts ->
            if (parts.size == 2) "${parts[0]}-r${parts[1]}" else tag
        }

    private fun File.strings(): Map<String, String> =
        Regex("""<string name="([^"]+)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
            .findAll(readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private fun untranslatable(): Set<String> =
        Regex("""<string name="([^"]+)" translatable="false"""")
            .findAll(File(resources, "values/strings.xml").readText())
            .map { it.groupValues[1] }
            .toSet()

    private fun placeholders(text: String): List<String> =
        Regex("""%\d+\$[sd]""").findAll(text).map { it.value }.sorted().toList()
}
