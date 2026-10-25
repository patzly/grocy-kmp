package com.patrickzedler.grocy.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DemoServersTest {

    @Test
    fun everyLanguageOfTheAppHasADemo() {
        val missing = AppLanguages.filter { demoServerFor(it) == null }

        assertEquals(emptyList(), missing)
    }

    @Test
    fun demosAreUniqueHttpsAddressesOfGrocy() {
        assertEquals(DemoServers.size, DemoServers.map { it.url }.toSet().size)
        assertEquals(DemoServers.size, DemoServers.map { it.languageTag }.toSet().size)
        assertTrue(
            DemoServers.all {
                Regex("""https://[a-z-]+\.demo\.grocy\.info""").matches(it.url)
            },
        )
    }

    @Test
    fun theDefaultDemoIsTheEnglishOne() {
        assertEquals(ServerConnection.Demo.DEFAULT_URL, demoServerFor("en")?.url)
    }

    @Test
    fun regionalTagUsesItsOwnDemoOrTheBaseLanguagesOne() {
        assertEquals("https://pt-br.demo.grocy.info", demoServerFor("pt-BR")?.url)
        assertEquals("https://pt-pt.demo.grocy.info", demoServerFor("pt_pt")?.url)
        assertEquals("https://de.demo.grocy.info", demoServerFor("de-CH")?.url)
    }

    @Test
    fun otherCodesOfALanguageFindItsDemo() {
        assertEquals("https://no.demo.grocy.info", demoServerFor("no")?.url)
        assertEquals("https://no.demo.grocy.info", demoServerFor("nb-NO")?.url)
        assertEquals("https://he-il.demo.grocy.info", demoServerFor("iw")?.url)
    }

    @Test
    fun languageWithoutDemoHasNone() {
        assertNull(demoServerFor("ar"))
        assertNull(demoServerFor(null))
        // Chinese has two demos, neither is the language as such
        assertNull(demoServerFor("zh"))
    }

    @Test
    fun demoIsFoundByItsAddress() {
        assertEquals("de", demoServerOf("https://de.demo.grocy.info/")?.languageTag)
        assertNotNull(demoServerOf(ServerConnection.Demo.DEFAULT_URL))
        assertNull(demoServerOf("https://grocy.example.com"))
    }
}
