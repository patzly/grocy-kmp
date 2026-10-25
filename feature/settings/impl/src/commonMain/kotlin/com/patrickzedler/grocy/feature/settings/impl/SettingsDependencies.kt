package com.patrickzedler.grocy.feature.settings.impl

import com.patrickzedler.grocy.core.data.auth.AuthRepository
import com.patrickzedler.grocy.core.data.language.AppLanguageStore
import com.patrickzedler.grocy.core.data.log.LogReader
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.data.server.ServerRepository
import com.patrickzedler.grocy.core.data.settings.AppearanceRepository
import com.patrickzedler.grocy.core.data.settings.SettingsStore
import com.patrickzedler.grocy.core.data.unlock.UnlockRepository

interface SettingsDependencies {
    val settingsStore: SettingsStore
    val appearanceRepository: AppearanceRepository
    val appLanguageStore: AppLanguageStore
    val unlockRepository: UnlockRepository

    /** Logcat on Android, the errors the web app recorded itself in browsers. */
    val logReader: LogReader

    val serverRepository: ServerRepository
    val authRepository: AuthRepository

    /** The user's settings on the server and the objects of their presets. */
    val masterDataRepository: MasterDataRepository

    /** After the configuration was reloaded, so every screen reads it anew. */
    fun restartApp()
}
