package com.patrickzedler.grocy.feature.settings.impl.general

import android.content.res.Resources

// The system's resources keep the device's language, the app's carry the chosen one
internal actual fun systemLanguageTag(): String? =
    Resources.getSystem().configuration.locales.get(0)?.toLanguageTag()
