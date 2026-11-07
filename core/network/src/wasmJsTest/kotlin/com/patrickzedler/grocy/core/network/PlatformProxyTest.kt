package com.patrickzedler.grocy.core.network

import kotlin.test.Test
import kotlin.test.assertFalse

class PlatformProxyTest {

    @Test
    fun browsersKeepTheirOwnProxySettings() {
        assertFalse(isProxySupported)
    }
}
