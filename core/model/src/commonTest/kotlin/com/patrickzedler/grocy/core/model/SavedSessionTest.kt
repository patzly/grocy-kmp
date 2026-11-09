package com.patrickzedler.grocy.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedSessionTest {

    private val url = "https://grocy.example.com"
    private val anna = SavedSession(ServerConnection.ApiKey(url, "a"), 1, "anna", "Anna A")
    private val ben = SavedSession(ServerConnection.ApiKey(url, "b"), 2, "ben", "Ben B")
    private val demo = SavedSession(ServerConnection.Demo(), 1, "demo", "Demo")

    @Test
    fun theSameUserWithAnotherKeyIsTheSameAccount() {
        val newKey = anna.copy(connection = ServerConnection.ApiKey(url, "new"))

        assertTrue(anna.isSameAs(newKey))
    }

    @Test
    fun anotherUserOfTheServerIsAnotherAccount() {
        assertFalse(anna.isSameAs(ben))
    }

    @Test
    fun everyDemoIsTheSameAccount() {
        val german = SavedSession(ServerConnection.Demo("https://de.demo.grocy.info"))

        assertTrue(demo.isSameAs(german))
    }

    @Test
    fun withoutKnownUsersTheCredentialsTell() {
        val first = SavedSession(ServerConnection.ApiKey(url, "a"))
        val second = SavedSession(ServerConnection.ApiKey(url, "b"))

        assertFalse(first.isSameAs(second))
        assertTrue(first.isSameAs(first.copy()))
    }

    @Test
    fun loggingInPutsTheSessionFirstAndKeepsNoDouble() {
        val saved = listOf(ben, anna, demo)

        val sessions = sessionsAfterLogin(
            saved,
            anna.connection,
            User(1, "anna", "Anna A"),
        )

        assertEquals(listOf(anna, ben, demo), sessions)
    }

    @Test
    fun theLastSessionBeforeTheCurrentOneComesRightAfterIt() {
        val first = sessionsAfterLogin(emptyList(), ben.connection, User(2, "ben", "Ben B"))
        val second = sessionsAfterLogin(first, demo.connection, User(1, "demo", "Demo"))
        val third = sessionsAfterLogin(second, anna.connection, User(1, "anna", "Anna A"))

        assertEquals(listOf(anna, demo, ben), third)
    }

    @Test
    fun aLoginWithoutAKnownUserKeepsWhatWasKnown() {
        assertEquals(listOf(anna), sessionsAfterLogin(listOf(anna), anna.connection, null))
    }

    @Test
    fun aLoginOlderThanTheSessionsIsListed() {
        val sessions = sessionsOf(anna.connection, listOf(ben))

        assertEquals(listOf(SavedSession(anna.connection), ben), sessions)
    }

    @Test
    fun whileLoggedOutTheOrderStays() {
        assertEquals(listOf(ben, anna), sessionsOf(null, listOf(ben, anna)))
    }

    @Test
    fun swipingReachesEverySessionAndComesBack() {
        var sessions = listOf(anna, ben, demo)
        val seen = mutableListOf(sessions.first())

        repeat(3) {
            val next = nextSession(sessions)!!
            seen += next
            // Opening one makes it the most recent, which must not change the way round
            sessions = listOf(next) + sessions.filterNot { it == next }
        }

        assertEquals(setOf(anna, ben, demo), seen.toSet())
        assertEquals(seen.first(), seen.last())
    }

    @Test
    fun aSingleSessionHasNoNext() {
        assertNull(nextSession(listOf(anna)))
    }

    @Test
    fun theCircleShowsTheUsernamesFirstLetter() {
        assertEquals("A", anna.initial)
    }

    @Test
    fun anUnknownUserShowsTheInstance() {
        val session = SavedSession(ServerConnection.ApiKey(url, "a"))

        assertEquals("grocy.example.com", session.name)
        assertEquals("G", session.initial)
    }

    @Test
    fun anEmojiStaysOneCharacter() {
        assertEquals("😀", anna.copy(username = "😀x").initial)
    }

    @Test
    fun secretsStayOutOfToString() {
        assertFalse("SECRET" in SavedSession(ServerConnection.ApiKey(url, "SECRET")).toString())
    }
}
