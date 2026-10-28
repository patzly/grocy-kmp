package com.patrickzedler.grocy.feature.settings.impl

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.patrickzedler.grocy.core.data.masterdata.MasterDataRepository
import com.patrickzedler.grocy.core.model.server.UserSettingValue
import com.patrickzedler.grocy.core.model.text.decimalPlaces
import com.patrickzedler.grocy.core.model.text.formatDecimal
import com.patrickzedler.grocy.core.model.text.parseDecimal
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.data_save_failed_details
import com.patrickzedler.grocy.core.resources.data_save_offline
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

/** Why a change of a user setting was not saved; the page shows the stored value again. */
internal sealed interface UserSettingEvent {
    data object Offline : UserSettingEvent

    data class Failed(val serverMessage: String?) : UserSettingEvent
}

/**
 * Saves the user settings of a page like Grocy's settings pages: each change on its own, one
 * after another, online only; a failed one is reverted and reported through [events].
 */
internal class UserSettingSaver(
    private val repository: MasterDataRepository,
    private val scope: CoroutineScope,
) {
    private val saving = Mutex()
    private val eventChannel = Channel<UserSettingEvent>(Channel.BUFFERED)
    val events: Flow<UserSettingEvent> = eventChannel.receiveAsFlow()

    /** Saves [value] as [key] after the changes before it; [revert] shows the stored value. */
    fun save(key: String, value: UserSettingValue, revert: () -> Unit) {
        scope.launch {
            val event = saving.withLock {
                try {
                    repository.saveUserSetting(key, value)
                    null
                } catch (e: CancellationException) {
                    throw e
                } catch (_: GrocyApiException.Unreachable) {
                    UserSettingEvent.Offline
                } catch (e: GrocyApiException.HttpError) {
                    UserSettingEvent.Failed(e.serverMessage)
                } catch (_: GrocyApiException) {
                    UserSettingEvent.Failed(serverMessage = null)
                }
            }
            if (event != null) {
                revert()
                eventChannel.send(event)
            }
        }
    }
}

/** Shows the failed saves of [events] in [snackbarHostState]. */
@Composable
internal fun UserSettingEventEffect(
    events: Flow<UserSettingEvent>,
    snackbarHostState: SnackbarHostState,
) {
    LaunchedEffect(events) {
        events.collect { event ->
            snackbarHostState.showSnackbar(
                when (event) {
                    UserSettingEvent.Offline -> getString(Res.string.data_save_offline)
                    is UserSettingEvent.Failed -> listOfNotNull(
                        getString(Res.string.data_save_failed),
                        event.serverMessage?.let {
                            getString(Res.string.data_save_failed_details, it)
                        },
                    ).joinToString("\n")
                },
            )
        }
    }
}

/** Why a typed number of a setting is not saved. */
internal sealed interface SettingError {
    /** Grocy's number message: at least [min] (at most [max]), up to [decimals] places. */
    data class InvalidNumber(val min: String, val max: String?, val decimals: Int) : SettingError
}

/**
 * Checks [text] like Grocy's number picker: at least [min], at most [max] if given, with up to
 * [places] decimal places; it also names the range for an empty field. Null when it is fine.
 */
internal fun numberSettingError(
    text: String,
    min: Double,
    max: Double?,
    places: Int,
): SettingError? {
    val value = parseDecimal(text)
    val invalid = value == null || value < min || max?.let { value > it } == true ||
        decimalPlaces(text) > places
    return if (invalid) {
        SettingError.InvalidNumber(
            min = formatDecimal(min, places),
            max = max?.let { formatDecimal(it, 0) },
            decimals = places,
        )
    } else {
        null
    }
}
