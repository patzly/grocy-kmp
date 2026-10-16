package com.patrickzedler.grocy.feature.settings.impl

import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.log.LogReader
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository

interface SettingsDependencies {
    val settingsStore: SettingsStore
    val appLanguageStore: AppLanguageStore
    val unlockRepository: UnlockRepository

    /** Null where the platform has no log for the user to see, which hides the log screen. */
    val logReader: LogReader?
}
