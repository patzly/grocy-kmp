package com.patrickzedler.grocy.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

class BackStackCodecTest {

    @Serializable
    @SerialName("home")
    private data object HomeRoute : Route

    @Serializable
    @SerialName("settings/general")
    private data object GeneralRoute : Route

    @Serializable
    @SerialName("product")
    private data class ProductRoute(val id: Int) : Route

    @Serializable
    @SerialName("userfields")
    private data class UserfieldsRoute(
        val entity: String? = null,
        val showDisabled: Boolean = false,
    ) : Route

    private val codec = BackStackCodec(
        SerializersModule {
            polymorphic(NavKey::class) {
                subclass(HomeRoute::class, HomeRoute.serializer())
                subclass(GeneralRoute::class, GeneralRoute.serializer())
                subclass(ProductRoute::class, ProductRoute.serializer())
                subclass(UserfieldsRoute::class, UserfieldsRoute.serializer())
            }
        },
    )

    @Test
    fun backStackSurvivesTheRoundTrip() {
        val backStack = listOf(HomeRoute, ProductRoute(id = 7), GeneralRoute)

        assertEquals(backStack, codec.decode(codec.encode(backStack)))
    }

    @Test
    fun storedBackStackStaysReadable() {
        // Written into the browser history of open tabs, so the format must not change
        val stored = """[{"type":"home"},{"type":"product","id":7}]"""

        assertEquals(listOf(HomeRoute, ProductRoute(id = 7)), codec.decode(stored))
    }

    @Test
    fun unknownRoutesAreNoBackStack() {
        assertNull(codec.decode("""[{"type":"home"},{"type":"removed"}]"""))
    }

    @Test
    fun garbageIsNoBackStack() {
        assertNull(codec.decode("not json"))
        assertNull(codec.decode("[]"))
        assertNull(codec.decode("""{"type":"home"}"""))
    }

    @Test
    fun hashNamesTheTopScreen() {
        assertEquals("#/settings/general", codec.hash(listOf(HomeRoute, GeneralRoute)))
    }

    @Test
    fun hashShowsTheArgumentsOfTheTopScreen() {
        assertEquals("#/product?id=12", codec.hash(listOf(HomeRoute, ProductRoute(12))))
    }

    @Test
    fun hashLeavesOutArgumentsAtTheirDefault() {
        assertEquals("#/userfields", codec.hash(listOf(HomeRoute, UserfieldsRoute())))
    }

    @Test
    fun hashEncodesTextArguments() {
        val route = UserfieldsRoute(entity = "userentity-Bücher & Co", showDisabled = true)

        assertEquals(
            "#/userfields?entity=userentity-B%C3%BCcher%20%26%20Co&showDisabled=true",
            codec.hash(listOf(HomeRoute, route)),
        )
    }

    @Test
    fun firstScreenKeepsThePlainAddress() {
        assertEquals("", codec.hash(listOf(HomeRoute)))
    }
}
