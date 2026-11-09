package com.patrickzedler.grocy.apps.shared

import android.app.Notification
import android.content.Intent
import com.patrickzedler.grocy.core.navigation.Route
import com.patrickzedler.grocy.feature.settings.api.NotificationSettingsRoute
import com.patrickzedler.grocy.feature.settings.api.SettingsRoute

/**
 * The page of the app that Android's app info opens with [action] and [categories], null for any
 * other start: its item leads to the settings, the one of its notification page to theirs.
 */
internal fun systemSettingsRouteOf(action: String?, categories: Set<String>?): Route? = when {
    action == Intent.ACTION_APPLICATION_PREFERENCES -> SettingsRoute
    action == Intent.ACTION_MAIN &&
        categories?.contains(Notification.INTENT_CATEGORY_NOTIFICATION_PREFERENCES) == true ->
        NotificationSettingsRoute
    else -> null
}
