package com.patrickzedler.grocy.core.model

data class UnlockState(
    val isKeyInstalled: Boolean,
    val isPlayStoreInstalled: Boolean,
    val checkUnlockKey: Boolean,
)
