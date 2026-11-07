package com.patrickzedler.grocy.core.network

import kotlin.test.Test
import kotlin.test.assertTrue

class PlatformProxyTest {

    @Test
    fun androidChoosesTheWayOfItsRequests() {
        assertTrue(isProxySupported)
    }
}
