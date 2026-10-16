package com.patrickzedler.grocy.core.data.language

import com.patrickzedler.grocy.core.data.InMemoryStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WebAppLanguageStoreTest {

    private val storage = InMemoryStorage()
    private var reloads = 0

    private fun store() = WebAppLanguageStore(storage, reload = { reloads++ })

    @Test
    fun systemLanguageByDefault() {
        assertNull(store().language.value)
    }

    @Test
    fun chosenLanguageIsStoredForIndexHtmlAndReloads() {
        val store = store()

        store.setLanguage("de")

        assertEquals("de", storage.values[WebAppLanguageStore.LANGUAGE])
        assertEquals("de", store.language.value)
        assertEquals(1, reloads)
    }

    @Test
    fun systemLanguageRemovesTheStoredOne() {
        storage.set(WebAppLanguageStore.LANGUAGE, "de")
        val store = store()

        store.setLanguage(null)

        assertNull(storage.values[WebAppLanguageStore.LANGUAGE])
        assertEquals(1, reloads)
    }

    @Test
    fun sameLanguageDoesNotReload() {
        storage.set(WebAppLanguageStore.LANGUAGE, "en")

        store().setLanguage("en")

        assertEquals(0, reloads)
    }
}
