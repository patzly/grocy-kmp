package com.patrickzedler.grocy.feature.login.impl

import com.patrickzedler.grocy.core.network.GrocyApiException
import kotlin.test.Test
import kotlin.test.assertEquals

class LoginErrorTest {

    @Test
    fun mapsApiErrorsToLoginErrors() {
        assertEquals(
            LoginError.Unreachable,
            GrocyApiException.Unreachable(RuntimeException()).toLoginError(),
        )
        assertEquals(LoginError.Unauthorized, GrocyApiException.Unauthorized().toLoginError())
        assertEquals(LoginError.NotGrocyServer, GrocyApiException.NotGrocyServer().toLoginError())
    }

    @Test
    fun mapsHomeAssistantErrors() {
        assertEquals(
            LoginError.HomeAssistantTokenRejected,
            GrocyApiException.HomeAssistantTokenRejected().toLoginError(),
        )
        assertEquals(LoginError.AddonNotFound, GrocyApiException.AddonNotFound().toLoginError())
        assertEquals(
            LoginError.NotHomeAssistant,
            GrocyApiException.NotHomeAssistant().toLoginError(),
        )
    }

    @Test
    fun missingEndpointMeansNoGrocyServer() {
        assertEquals(LoginError.NotGrocyServer, GrocyApiException.HttpError(404).toLoginError())
    }

    @Test
    fun otherFailuresAreUnknown() {
        assertEquals(LoginError.Unknown, GrocyApiException.HttpError(500).toLoginError())
        assertEquals(LoginError.Unknown, IllegalStateException().toLoginError())
    }
}
