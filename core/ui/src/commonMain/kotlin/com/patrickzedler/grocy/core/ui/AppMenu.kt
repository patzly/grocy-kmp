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

/** Where the app menu's Settings and About lead, the app's own screens. */
class AppMenuLinks(
    val onSettingsClick: () -> Unit,
    val onAboutClick: () -> Unit,
)

/** The [AppMenuLinks] of [appMenuItems], provided at the app's root. */
val LocalAppMenuLinks = staticCompositionLocalOf<AppMenuLinks?> { null }

/** The overflow menu of the app's screens and its open dialog; see [rememberAppMenu]. */
@Stable
class AppMenu internal constructor(
    val items: List<AppBarMenuItem>,
    internal val dialog: MutableState<AppDialog?>,
)

/**
 * Settings, About, Help and Send feedback: the overflow menu of every screen except About and
 * the settings, which have their own. Pass [AppMenu.items] to the screen's app bar and show
 * [AppMenuDialogs] for Help and feedback. Without [onSettingsClick] and [onAboutClick] only Help
 * and Send feedback remain, e.g. for forms, whose menu is about their object.
 */
@Composable
fun rememberAppMenu(
    onSettingsClick: (() -> Unit)? = null,
    onAboutClick: (() -> Unit)? = null,
): AppMenu {
    // Saveable, so an open dialog survives rotating the device
    val dialog = rememberSaveable { mutableStateOf<AppDialog?>(null) }
    val currentOnSettingsClick by rememberUpdatedState(onSettingsClick)
    val currentOnAboutClick by rememberUpdatedState(onAboutClick)
    val hasSettings = onSettingsClick != null
    val hasAbout = onAboutClick != null
    val settings = stringResource(Res.string.title_settings)
    val about = stringResource(Res.string.title_about)
    val help = stringResource(Res.string.title_help)
    val feedback = stringResource(Res.string.action_send_feedback)
    return remember(dialog, hasSettings, hasAbout, settings, about, help, feedback) {
        AppMenu(
            items = listOfNotNull(
                AppBarMenuItem(settings) { currentOnSettingsClick?.invoke() }
                    .takeIf { hasSettings },
                AppBarMenuItem(about) { currentOnAboutClick?.invoke() }.takeIf { hasAbout },
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

/**
 * The full app menu of a screen, Settings and About leading to [LocalAppMenuLinks], with the
 * dialogs of Help and Send feedback.
 */
@Composable
fun appMenuItems(): List<AppBarMenuItem> {
    val links = LocalAppMenuLinks.current
    val menu = rememberAppMenu(
        onSettingsClick = links?.onSettingsClick,
        onAboutClick = links?.onAboutClick,
    )
    AppMenuDialogs(menu)
    return menu.items
}
