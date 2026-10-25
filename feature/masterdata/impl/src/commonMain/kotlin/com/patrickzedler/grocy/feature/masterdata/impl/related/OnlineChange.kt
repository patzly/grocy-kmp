package com.patrickzedler.grocy.feature.masterdata.impl.related

import androidx.compose.runtime.Composable
import com.patrickzedler.grocy.core.network.GrocyApiException
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.data_save_failed
import com.patrickzedler.grocy.core.resources.data_save_failed_details
import com.patrickzedler.grocy.core.resources.data_save_offline
import com.patrickzedler.grocy.core.resources.objects_action_offline
import kotlin.coroutines.cancellation.CancellationException
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/** Why a change on the server failed; changes are online only. */
internal sealed interface ChangeError {
    data object Offline : ChangeError

    /** The server rejected it, [serverMessage] is Grocy's explanation if it gave one. */
    data class Failed(val serverMessage: String?) : ChangeError
}

/** Runs [change] on the server; null when it succeeded, otherwise why it failed. */
internal suspend fun changeErrorOf(change: suspend () -> Unit): ChangeError? = try {
    change()
    null
} catch (e: CancellationException) {
    throw e
} catch (_: GrocyApiException.Unreachable) {
    ChangeError.Offline
} catch (e: GrocyApiException.HttpError) {
    ChangeError.Failed(e.serverMessage)
} catch (_: GrocyApiException) {
    ChangeError.Failed(serverMessage = null)
}

/** The message of a failed save, with Grocy's explanation. */
@Composable
internal fun ChangeError.saveMessage(): String = when (this) {
    ChangeError.Offline -> stringResource(Res.string.data_save_offline)
    is ChangeError.Failed -> listOfNotNull(
        stringResource(Res.string.data_save_failed),
        serverMessage?.let { stringResource(Res.string.data_save_failed_details, it) },
    ).joinToString("\n")
}

/** The message of another failed action, [failure] naming it, e.g. for a snackbar. */
internal suspend fun ChangeError.actionMessage(failure: StringResource): String = when (this) {
    ChangeError.Offline -> getString(Res.string.objects_action_offline)
    is ChangeError.Failed -> listOfNotNull(
        getString(failure),
        serverMessage?.let { getString(Res.string.data_save_failed_details, it) },
    ).joinToString("\n")
}
