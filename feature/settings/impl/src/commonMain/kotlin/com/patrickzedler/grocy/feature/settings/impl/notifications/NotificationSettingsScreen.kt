package com.patrickzedler.grocy.feature.settings.impl.notifications

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.data.notifications.ShowNowResult
import com.patrickzedler.grocy.core.design.withTabularNumbers
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedSwitchItem
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.design.component.showNewestSnackbar
import com.patrickzedler.grocy.core.forms.PickTimeDialog
import com.patrickzedler.grocy.core.forms.clockTimeText
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.notifications.NotificationTopic
import com.patrickzedler.grocy.core.model.notifications.ShoppingListNotificationSettings
import com.patrickzedler.grocy.core.model.notifications.ShoppingListNotificationType
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.chores_status_due_soon
import com.patrickzedler.grocy.core.resources.chores_status_due_today
import com.patrickzedler.grocy.core.resources.chores_status_overdue
import com.patrickzedler.grocy.core.resources.ic_rounded_schedule
import com.patrickzedler.grocy.core.resources.ic_rounded_warning
import com.patrickzedler.grocy.core.resources.masterdata_batteries
import com.patrickzedler.grocy.core.resources.masterdata_chores
import com.patrickzedler.grocy.core.resources.meal_plan_title
import com.patrickzedler.grocy.core.resources.notifications_battery
import com.patrickzedler.grocy.core.resources.notifications_battery_hint
import com.patrickzedler.grocy.core.resources.notifications_blocked
import com.patrickzedler.grocy.core.resources.notifications_blocked_browser
import com.patrickzedler.grocy.core.resources.notifications_blocked_request
import com.patrickzedler.grocy.core.resources.notifications_blocked_settings
import com.patrickzedler.grocy.core.resources.notifications_browser
import com.patrickzedler.grocy.core.resources.notifications_browser_hint
import com.patrickzedler.grocy.core.resources.notifications_daily
import com.patrickzedler.grocy.core.resources.notifications_exact
import com.patrickzedler.grocy.core.resources.notifications_exact_hint
import com.patrickzedler.grocy.core.resources.notifications_nothing
import com.patrickzedler.grocy.core.resources.notifications_only_mine
import com.patrickzedler.grocy.core.resources.notifications_shopping_hint
import com.patrickzedler.grocy.core.resources.notifications_shopping_when_added
import com.patrickzedler.grocy.core.resources.notifications_shopping_when_checked_off
import com.patrickzedler.grocy.core.resources.notifications_show_failed
import com.patrickzedler.grocy.core.resources.notifications_show_now
import com.patrickzedler.grocy.core.resources.notifications_show_now_hint
import com.patrickzedler.grocy.core.resources.notifications_time
import com.patrickzedler.grocy.core.resources.product_group_stock
import com.patrickzedler.grocy.core.resources.settings_notifications
import com.patrickzedler.grocy.core.resources.shopping_list_list
import com.patrickzedler.grocy.core.resources.stock_entry_due_soon
import com.patrickzedler.grocy.core.resources.stock_entry_expired
import com.patrickzedler.grocy.core.resources.stock_entry_overdue
import com.patrickzedler.grocy.core.resources.stock_overview_below_min_stock
import com.patrickzedler.grocy.core.resources.tasks_title
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuActions
import com.patrickzedler.grocy.feature.settings.impl.SettingsMenuViewModel
import com.patrickzedler.grocy.feature.settings.impl.SettingsScaffold
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * The daily notifications: what keeps them from coming on time, if anything, then a group per
 * feature of the server. The permission is asked for when the user turns one on.
 */
@Composable
internal fun NotificationSettingsScreen(
    viewModel: NotificationSettingsViewModel,
    menuViewModel: SettingsMenuViewModel,
    menuActions: SettingsMenuActions,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    pane: ScreenPane? = null,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val config by viewModel.serverConfig.collectAsStateWithLifecycle()
    val shoppingList by viewModel.shoppingListSettings.collectAsStateWithLifecycle()
    val access = rememberNotificationAccess()
    val snackbarHostState = remember { SnackbarHostState() }

    // Saved: the activity may be recreated while the system's question is open
    var pendingKind by rememberSaveable { mutableStateOf<NotificationKind?>(null) }
    var pendingShowNow by rememberSaveable { mutableStateOf<NotificationKind?>(null) }
    var pendingShoppingList by rememberSaveable {
        mutableStateOf<ShoppingListNotificationType?>(null)
    }
    var timePickerKind by rememberSaveable { mutableStateOf<NotificationKind?>(null) }

    LaunchedEffect(access.permission, pendingKind, pendingShowNow, pendingShoppingList) {
        if (access.permission != NotificationPermission.Granted) return@LaunchedEffect
        pendingShoppingList?.let { viewModel.setShoppingList(it, true) }
        pendingShoppingList = null
        pendingKind?.let { viewModel.setEnabled(it, true) }
        pendingShowNow?.let(viewModel::showNow)
        pendingKind = null
        pendingShowNow = null
    }
    LaunchedEffect(viewModel) {
        viewModel.showNowProblems.collectLatest { problem ->
            snackbarHostState.showNewestSnackbar(
                getString(
                    if (problem == ShowNowResult.FAILED) {
                        Res.string.notifications_show_failed
                    } else {
                        Res.string.notifications_nothing
                    },
                ),
            )
        }
    }

    // Asks when it can; where only the settings can change it, the warning above says so
    fun requestAccess() {
        when {
            access.permission == NotificationPermission.Requestable -> access.request()
            access.canOpenSettings -> access.openSettings()
        }
    }

    SettingsScaffold(
        title = stringResource(Res.string.settings_notifications),
        menuViewModel = menuViewModel,
        menuActions = menuActions,
        onBackClick = onBackClick,
        modifier = modifier,
        pane = pane,
        snackbarHostState = snackbarHostState,
    ) {
        // Empty for the moment until the stored settings are read
        val current = settings
        val currentConfig = config
        if (current != null && currentConfig != null) {
            val shoppingListNotifications = shoppingList ?: ShoppingListNotificationSettings()
            notificationNotices(
                access = access,
                settings = current,
                isShoppingListOn = shoppingListNotifications.appliesTo(currentConfig),
                onRequest = ::requestAccess,
            )
            shownNotificationKinds(currentConfig).forEach { kind ->
                notificationGroup(
                    kind = kind,
                    settings = current,
                    config = currentConfig,
                    viewModel = viewModel,
                    onEnable = {
                        if (access.permission == NotificationPermission.Granted) {
                            viewModel.setEnabled(kind, true)
                        } else {
                            pendingKind = kind
                            requestAccess()
                        }
                    },
                    onShowNow = {
                        if (access.permission == NotificationPermission.Granted) {
                            viewModel.showNow(kind)
                        } else {
                            pendingShowNow = kind
                            requestAccess()
                        }
                    },
                    onTimeClick = { timePickerKind = kind },
                )
            }
            if (currentConfig.stock && currentConfig.shoppingList) {
                shoppingListGroup(
                    settings = shoppingListNotifications,
                    onChange = { type, enabled ->
                        if (!enabled || access.permission == NotificationPermission.Granted) {
                            viewModel.setShoppingList(type, enabled)
                        } else {
                            pendingShoppingList = type
                            requestAccess()
                        }
                    },
                )
            }
        }
    }

    val pickerKind = timePickerKind
    val minuteOfDay = pickerKind?.let { settings?.get(it)?.minuteOfDay }
    if (pickerKind != null && minuteOfDay != null) {
        PickTimeDialog(
            initial = minuteOfDay / MINUTES_PER_HOUR to minuteOfDay % MINUTES_PER_HOUR,
            onPick = { hour, minute ->
                timePickerKind = null
                viewModel.setTime(pickerKind, hour, minute)
            },
            onDismissRequest = { timePickerKind = null },
        )
    }
}

/** What keeps notifications from coming or from coming on time, only once it matters. */
private fun LazyListScope.notificationNotices(
    access: NotificationAccess,
    settings: NotificationSettings,
    isShoppingListOn: Boolean,
    onRequest: () -> Unit,
) {
    val anyDailyEnabled = settings.enabledKinds.isNotEmpty()
    val anyEnabled = anyDailyEnabled || isShoppingListOn
    val isGranted = access.permission == NotificationPermission.Granted
    if (access.permission == NotificationPermission.Denied || (anyEnabled && !isGranted)) {
        segmentedGroup(key = "blocked") {
            val text = when {
                access.permission == NotificationPermission.Requestable ->
                    Res.string.notifications_blocked_request
                access.canOpenSettings -> Res.string.notifications_blocked_settings
                else -> Res.string.notifications_blocked_browser
            }
            NoticeItem(
                title = Res.string.notifications_blocked,
                text = text,
                index = 0,
                count = 1,
                isError = true,
                onClick = onRequest.takeIf {
                    access.canOpenSettings ||
                        access.permission == NotificationPermission.Requestable
                },
            )
        }
    }
    if (access.onlyWhileOpen) {
        segmentedGroup(key = "browser") {
            NoticeItem(
                title = Res.string.notifications_browser,
                text = Res.string.notifications_browser_hint,
                index = 0,
                count = 1,
            )
        }
    }
    // Only the daily ones have a time to keep
    val showExact = anyDailyEnabled && access.exactAlarmsBlocked
    val showBattery = anyEnabled && access.batteryOptimized
    if (showExact || showBattery) {
        segmentedGroup(key = "on_time") {
            val count = listOf(showExact, showBattery).count { it }
            if (showExact) {
                NoticeItem(
                    title = Res.string.notifications_exact,
                    text = Res.string.notifications_exact_hint,
                    index = 0,
                    count = count,
                    onClick = access::openExactAlarmSettings,
                )
            }
            if (showBattery) {
                NoticeItem(
                    title = Res.string.notifications_battery,
                    text = Res.string.notifications_battery_hint,
                    index = count - 1,
                    count = count,
                    onClick = access::openBatterySettings,
                )
            }
        }
    }
}

/** What others do on the shopping lists; there is nothing that gets due, so no time either. */
private fun LazyListScope.shoppingListGroup(
    settings: ShoppingListNotificationSettings,
    onChange: (ShoppingListNotificationType, Boolean) -> Unit,
) {
    segmentedGroup(key = "shopping_list", title = Res.string.shopping_list_list) {
        SegmentedSwitchItem(
            headline = stringResource(Res.string.notifications_shopping_when_added),
            supporting = stringResource(Res.string.notifications_shopping_hint),
            checked = ShoppingListNotificationType.ADDED in settings,
            onCheckedChange = { onChange(ShoppingListNotificationType.ADDED, it) },
            index = 0,
            count = 2,
        )
        SegmentedSwitchItem(
            headline = stringResource(Res.string.notifications_shopping_when_checked_off),
            checked = ShoppingListNotificationType.CHECKED_OFF in settings,
            onCheckedChange = { onChange(ShoppingListNotificationType.CHECKED_OFF, it) },
            index = 1,
            count = 2,
        )
    }
}

private fun LazyListScope.notificationGroup(
    kind: NotificationKind,
    settings: NotificationSettings,
    config: ServerConfig,
    viewModel: NotificationSettingsViewModel,
    onEnable: () -> Unit,
    onShowNow: () -> Unit,
    onTimeClick: () -> Unit,
) {
    segmentedGroup(key = kind.name, title = kind.title) {
        val current = settings[kind]
        val topics = shownNotificationTopics(kind, config)
        val canOnlyMine = canNotifyOnlyMine(kind, config)
        // Time, topics, assigned and showing now follow only while it is on
        val count = if (current.enabled) {
            topics.size + if (canOnlyMine) FIXED_ITEMS + 1 else FIXED_ITEMS
        } else {
            1
        }
        var index = 0
        SegmentedSwitchItem(
            headline = stringResource(Res.string.notifications_daily),
            checked = current.enabled,
            onCheckedChange = { enabled ->
                if (enabled) onEnable() else viewModel.setEnabled(kind, false)
            },
            index = index++,
            count = count,
        )
        if (current.enabled) {
            TimeItem(current.minuteOfDay, index++, count, onTimeClick)
            topics.forEach { topic ->
                SegmentedSwitchItem(
                    headline = stringResource(topic.label),
                    checked = topic in current.topics,
                    onCheckedChange = { viewModel.setTopic(topic, it) },
                    index = index++,
                    count = count,
                )
            }
            if (canOnlyMine) {
                SegmentedSwitchItem(
                    headline = stringResource(Res.string.notifications_only_mine),
                    checked = current.onlyMine,
                    onCheckedChange = { viewModel.setOnlyMine(kind, it) },
                    index = index++,
                    count = count,
                )
            }
            NoticeItem(
                title = Res.string.notifications_show_now,
                text = Res.string.notifications_show_now_hint,
                index = index,
                count = count,
                icon = false,
                isLoading = viewModel.showingNow == kind,
                onClick = onShowNow,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimeItem(minuteOfDay: Int, index: Int, count: Int, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper {
                Icon(painterResource(Res.drawable.ic_rounded_schedule), contentDescription = null)
            }
        },
        supportingContent = {
            Text(
                text = clockTimeText(
                    minuteOfDay / MINUTES_PER_HOUR,
                    minuteOfDay % MINUTES_PER_HOUR,
                ),
                style = MaterialTheme.typography.bodyMedium.withTabularNumbers(),
            )
        },
        content = { Text(stringResource(Res.string.notifications_time)) },
    )
}

/** A note or, with [onClick], something to do about it; [isError] in the error colors. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NoticeItem(
    title: StringResource,
    text: StringResource,
    index: Int,
    count: Int,
    isError: Boolean = false,
    icon: Boolean = isError,
    isLoading: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    val leading: (@Composable () -> Unit)? = if (icon) {
        {
            LeadingTrailingContentWrapper {
                Icon(painterResource(Res.drawable.ic_rounded_warning), contentDescription = null)
            }
        }
    } else {
        null
    }
    val supporting = @Composable {
        // Comes up after the user's answer to the system's question
        Text(
            text = stringResource(text),
            modifier = Modifier.semantics {
                if (isError) liveRegion = LiveRegionMode.Polite
            },
        )
    }
    val colors = if (isError) {
        val contentColor = MaterialTheme.colorScheme.onErrorContainer
        ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = contentColor,
            leadingContentColor = contentColor,
            supportingContentColor = contentColor,
        )
    } else {
        SegmentedListDefaults.colors()
    }
    if (onClick != null) {
        SegmentedListItem(
            onClick = {
                haptic.confirm()
                onClick()
            },
            shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
            colors = colors,
            modifier = Modifier.fillMaxWidth().clickCursor(),
            verticalAlignment = Alignment.CenterVertically,
            leadingContent = leading,
            trailingContent = if (isLoading) {
                { LoadingIndicator(modifier = Modifier.size(40.dp)) }
            } else {
                null
            },
            supportingContent = supporting,
            content = { Text(stringResource(title)) },
        )
    } else {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            leadingContent = leading,
            supportingContent = supporting,
            shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
            colors = colors,
            content = { Text(stringResource(title)) },
        )
    }
}

private val NotificationKind.title: StringResource
    get() = when (this) {
        NotificationKind.STOCK -> Res.string.product_group_stock
        NotificationKind.CHORES -> Res.string.masterdata_chores
        NotificationKind.TASKS -> Res.string.tasks_title
        NotificationKind.BATTERIES -> Res.string.masterdata_batteries
        NotificationKind.MEAL_PLAN -> Res.string.meal_plan_title
    }

private val NotificationTopic.label: StringResource
    get() = when (this) {
        NotificationTopic.STOCK_DUE_SOON -> Res.string.stock_entry_due_soon
        NotificationTopic.STOCK_OVERDUE -> Res.string.stock_entry_overdue
        NotificationTopic.STOCK_EXPIRED -> Res.string.stock_entry_expired
        NotificationTopic.STOCK_BELOW_MIN -> Res.string.stock_overview_below_min_stock
        NotificationTopic.CHORES_OVERDUE,
        NotificationTopic.TASKS_OVERDUE,
        NotificationTopic.BATTERIES_OVERDUE,
        -> Res.string.chores_status_overdue
        NotificationTopic.CHORES_DUE_TODAY,
        NotificationTopic.TASKS_DUE_TODAY,
        NotificationTopic.BATTERIES_DUE_TODAY,
        -> Res.string.chores_status_due_today
        NotificationTopic.CHORES_DUE_SOON,
        NotificationTopic.TASKS_DUE_SOON,
        NotificationTopic.BATTERIES_DUE_SOON,
        -> Res.string.chores_status_due_soon
        NotificationTopic.MEAL_PLAN_TODAY -> Res.string.meal_plan_title
    }

// The switch, the time and showing now
private const val FIXED_ITEMS = 3
private const val MINUTES_PER_HOUR = 60
