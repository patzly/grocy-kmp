package com.patrickzedler.grocy.core.model

data class UnlockState(
    val isKeyInstalled: Boolean,
    val isPlayStoreInstalled: Boolean,
    val checkUnlockKey: Boolean,
) {
    /** Unlocked features are available: the key is installed or the check is turned off. */
    val isUnlocked: Boolean
        get() = isKeyInstalled || !checkUnlockKey
}
