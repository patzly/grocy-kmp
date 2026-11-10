package com.patrickzedler.grocy.feature.settings.impl.notifications

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.notifications.NotificationRepository
import com.patrickzedler.grocy.core.data.notifications.ShowNowResult
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationKindSettings
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.notifications.NotificationTopic
import com.patrickzedler.grocy.core.model.notifications.ShoppingListNotificationSettings
import com.patrickzedler.grocy.core.model.notifications.ShoppingListNotificationType
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The daily notifications per feature. Every change is saved at once and sets the alarm anew. */
internal class NotificationSettingsViewModel(
    private val repository: NotificationRepository,
    /** The features of the server: others have no settings here, they notify about nothing. */
    val serverConfig: StateFlow<ServerConfig?>,
    /** Which changes of the shopping lists notify, read at app start. */
    val shoppingListSettings: StateFlow<ShoppingListNotificationSettings?> =
        MutableStateFlow(ShoppingListNotificationSettings()),
    private val enableShoppingList: suspend (ShoppingListNotificationType, Boolean) -> Unit =
        { _, _ -> },
) : ViewModel() {

    /** Read at app start, so the page shows the stored values in its first frame. */
    val settings: StateFlow<NotificationSettings?> = repository.settings

    private val showNowProblemChannel = Channel<ShowNowResult>(Channel.BUFFERED)

    /** "Show now" had nothing to tell about or could not show it; success speaks for itself. */
    val showNowProblems: Flow<ShowNowResult> = showNowProblemChannel.receiveAsFlow()

    /** Whose "Show now" is downloading the current state, which can take a moment. */
    var showingNow by mutableStateOf<NotificationKind?>(null)
        private set

    fun setShoppingList(type: ShoppingListNotificationType, enabled: Boolean) {
        viewModelScope.launch { enableShoppingList(type, enabled) }
    }

    fun setEnabled(kind: NotificationKind, enabled: Boolean) =
        update(kind) { it.copy(enabled = enabled) }

    fun setTime(kind: NotificationKind, hour: Int, minute: Int) =
        update(kind) { it.copy(minuteOfDay = hour * MINUTES_PER_HOUR + minute) }

    fun setTopic(topic: NotificationTopic, enabled: Boolean) = update(topic.kind) {
        it.copy(topics = if (enabled) it.topics + topic else it.topics - topic)
    }

    fun setOnlyMine(kind: NotificationKind, onlyMine: Boolean) =
        update(kind) { it.copy(onlyMine = onlyMine) }

    fun showNow(kind: NotificationKind) {
        if (showingNow != null) return
        showingNow = kind
        viewModelScope.launch {
            val result = repository.showNow(kind)
            showingNow = null
            if (result != ShowNowResult.SHOWN) showNowProblemChannel.send(result)
        }
    }

    private fun update(
        kind: NotificationKind,
        transform: (NotificationKindSettings) -> NotificationKindSettings,
    ) {
        viewModelScope.launch {
            repository.update { it.with(kind, transform(it[kind])) }
        }
    }

    private companion object {
        const val MINUTES_PER_HOUR = 60
    }
}

/** The features [config] has, the only ones with settings here. */
internal fun shownNotificationKinds(config: ServerConfig): List<NotificationKind> =
    NotificationOrder.filter { it.isAvailable(config) }

// Like the start screen's default arrangement
private val NotificationOrder = listOf(
    NotificationKind.STOCK,
    NotificationKind.MEAL_PLAN,
    NotificationKind.CHORES,
    NotificationKind.TASKS,
    NotificationKind.BATTERIES,
)

/** What can be chosen a notification tells about: Grocy's meal plan has only one thing to tell. */
internal fun shownNotificationTopics(
    kind: NotificationKind,
    config: ServerConfig,
): List<NotificationTopic> =
    kind.topics.filter { it.isAvailable(config) }.takeIf { it.size > 1 }.orEmpty()

/** Whether Grocy assigns what [kind] tells about to users. */
internal fun canNotifyOnlyMine(kind: NotificationKind, config: ServerConfig): Boolean =
    when (kind) {
        NotificationKind.CHORES -> config.choresAssignments
        NotificationKind.TASKS -> true
        else -> false
    }
