package com.patrickzedler.grocy.feature.settings.impl.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrickzedler.grocy.core.data.notifications.NotificationRepository
import com.patrickzedler.grocy.core.model.notifications.NotificationKind
import com.patrickzedler.grocy.core.model.notifications.NotificationKindSettings
import com.patrickzedler.grocy.core.model.notifications.NotificationSettings
import com.patrickzedler.grocy.core.model.notifications.NotificationTopic
import com.patrickzedler.grocy.core.model.server.ServerConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** The daily notifications per feature. Every change is saved at once and sets the alarm anew. */
internal class NotificationSettingsViewModel(
    private val repository: NotificationRepository,
    /** The features of the server: others have no settings here, they notify about nothing. */
    val serverConfig: StateFlow<ServerConfig?>,
) : ViewModel() {

    /** Read at app start, so the page shows the stored values in its first frame. */
    val settings: StateFlow<NotificationSettings?> = repository.settings

    private val nothingToShowChannel = Channel<Unit>(Channel.BUFFERED)

    /** "Show now" found nothing to tell about. */
    val nothingToShow: Flow<Unit> = nothingToShowChannel.receiveAsFlow()

    fun setEnabled(kind: NotificationKind, enabled: Boolean) =
        update(kind) { it.copy(enabled = enabled) }

    fun setTime(kind: NotificationKind, hour: Int, minute: Int) =
        update(kind) { it.copy(minuteOfDay = hour * MINUTES_PER_HOUR + minute) }

    fun setTopic(topic: NotificationTopic, enabled: Boolean) = update(topic.kind) {
        it.copy(topics = if (enabled) it.topics + topic else it.topics - topic)
    }

    fun setShowNames(kind: NotificationKind, showNames: Boolean) =
        update(kind) { it.copy(showNames = showNames) }

    fun setOnlyMine(kind: NotificationKind, onlyMine: Boolean) =
        update(kind) { it.copy(onlyMine = onlyMine) }

    fun showNow(kind: NotificationKind) {
        viewModelScope.launch {
            if (!repository.showNow(kind)) nothingToShowChannel.send(Unit)
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
    NotificationKind.entries.filter { it.isAvailable(config) }

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
