package com.patrickzedler.grocy.core.data.credentials

import com.patrickzedler.grocy.core.data.InMemoryStorage
import com.patrickzedler.grocy.core.model.SavedSession
import com.patrickzedler.grocy.core.model.ServerConnection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class WebCredentialStoreTest {

    private val connection = ServerConnection.ApiKey("https://grocy.example.com", "secret")

    @Test
    fun startsLoggedOutWithEmptyStorage() = runTest {
        assertNull(WebCredentialStore(InMemoryStorage()).connection.first())
    }

    @Test
    fun readsConnectionStoredInAnEarlierSession() = runTest {
        val storage = InMemoryStorage(
            WebCredentialStore.STORAGE_KEY to ServerConnectionCodec.encode(connection),
        )

        assertEquals(connection, WebCredentialStore(storage).connection.first())
    }

    @Test
    fun unreadableStoredDataMeansLoggedOut() = runTest {
        val storage = InMemoryStorage(WebCredentialStore.STORAGE_KEY to "{broken")

        assertNull(WebCredentialStore(storage).connection.first())
    }

    @Test
    fun saveStoresAndEmitsConnection() = runTest {
        val storage = InMemoryStorage()
        val store = WebCredentialStore(storage)

        store.save(connection)

        assertEquals(connection, store.connection.first())
        assertEquals(connection, WebCredentialStore(storage).connection.first())
    }

    @Test
    fun clearRemovesConnection() = runTest {
        val storage = InMemoryStorage()
        val store = WebCredentialStore(storage)
        store.save(connection)

        store.clear()

        assertNull(store.connection.first())
        assertFalse(WebCredentialStore.STORAGE_KEY in storage.values)
    }

    @Test
    fun savedSessionsSurviveARestart() = runTest {
        val storage = InMemoryStorage()
        val sessions = listOf(SavedSession(connection, 1, "anna", "Anna"))

        WebCredentialStore(storage).saveSessions(sessions)

        assertEquals(sessions, WebCredentialStore(storage).sessions.first())
    }

    @Test
    fun unreadableSessionsMeanNone() = runTest {
        val storage = InMemoryStorage(WebCredentialStore.SESSIONS_STORAGE_KEY to "{broken")

        assertEquals(emptyList(), WebCredentialStore(storage).sessions.first())
    }
}
