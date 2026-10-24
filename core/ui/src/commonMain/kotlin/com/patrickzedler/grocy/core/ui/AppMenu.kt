package com.patrickzedler.grocy.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.model.UnlockState
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_send_feedback
import com.patrickzedler.grocy.core.resources.title_about
import com.patrickzedler.grocy.core.resources.title_help
import com.patrickzedler.grocy.core.resources.title_settings
import org.jetbrains.compose.resources.stringResource

/** The app's [UnlockState] for dialogs that show the Play extras, provided at the app's root. */
val LocalUnlockState = staticCompositionLocalOf<UnlockState?> { null }

/** The overflow menu of the app's screens and its open dialog; see [rememberAppMenu]. */
@Stable
class AppMenu internal constructor(
    val items: List<AppBarMenuItem>,
    internal val dialog: MutableState<AppDialog?>,
)

/**
 * Settings, About, Help and Send feedback: the overflow menu of every screen except About and
 * the settings, which have their own. Pass [AppMenu.items] to the screen's app bar and show
 * [AppMenuDialogs] for Help and feedback.
 */
@Composable
fun rememberAppMenu(onSettingsClick: () -> Unit, onAboutClick: () -> Unit): AppMenu {
    // Saveable, so an open dialog survives rotating the device
    val dialog = rememberSaveable { mutableStateOf<AppDialog?>(null) }
    val currentOnSettingsClick by rememberUpdatedState(onSettingsClick)
    val currentOnAboutClick by rememberUpdatedState(onAboutClick)
    val settings = stringResource(Res.string.title_settings)
    val about = stringResource(Res.string.title_about)
    val help = stringResource(Res.string.title_help)
    val feedback = stringResource(Res.string.action_send_feedback)
    return remember(dialog, settings, about, help, feedback) {
        AppMenu(
            items = listOf(
                AppBarMenuItem(settings) { currentOnSettingsClick() },
                AppBarMenuItem(about) { currentOnAboutClick() },
                AppBarMenuItem(help) { dialog.value = AppDialog.HELP },
                AppBarMenuItem(feedback) { dialog.value = AppDialog.FEEDBACK },
            ),
            dialog = dialog,
        )
    }
}

/** The dialogs [menu] opens. */
@Composable
fun AppMenuDialogs(menu: AppMenu) {
    var dialog by menu.dialog
    AppDialogHost(
        dialog = dialog,
        unlockState = LocalUnlockState.current,
        onDialogChange = { dialog = it },
    )
}
