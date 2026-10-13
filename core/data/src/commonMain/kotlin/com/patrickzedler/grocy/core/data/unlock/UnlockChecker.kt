package com.patrickzedler.grocy.core.data.unlock

interface UnlockChecker {

    fun isKeyInstalled(): Boolean

    fun isPlayStoreInstalled(): Boolean
}
