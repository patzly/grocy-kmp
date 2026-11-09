package com.patrickzedler.grocy.core.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.HapticTooltipBox
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.dialogListScrollbar
import com.patrickzedler.grocy.core.model.SavedSession
import com.patrickzedler.grocy.core.model.ServerConnection
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_cancel
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.action_delete
import com.patrickzedler.grocy.core.resources.ic_rounded_delete
import com.patrickzedler.grocy.core.resources.ic_rounded_logout
import com.patrickzedler.grocy.core.resources.login_error_certificate
import com.patrickzedler.grocy.core.resources.login_error_unauthorized
import com.patrickzedler.grocy.core.resources.login_error_unknown
import com.patrickzedler.grocy.core.resources.login_error_unreachable
import com.patrickzedler.grocy.core.resources.sessions_other
import com.patrickzedler.grocy.core.resources.sessions_remove
import com.patrickzedler.grocy.core.resources.sessions_remove_message
import com.patrickzedler.grocy.core.resources.sessions_remove_title
import com.patrickzedler.grocy.core.resources.sessions_title
import com.patrickzedler.grocy.core.resources.settings_server_logout
import com.patrickzedler.grocy.core.resources.settings_server_logout_description
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** What the user reads when another session could not be opened. */
val SessionError.message: StringResource
    get() = when (this) {
        SessionError.UNREACHABLE -> Res.string.login_error_unreachable
        SessionError.REJECTED -> Res.string.login_error_unauthorized
        SessionError.UNTRUSTED -> Res.string.login_error_certificate
        SessionError.FAILED -> Res.string.login_error_unknown
    }

/** A session's profile circle: the first letter of its username. Decorative, its name is beside. */
@Composable
fun SessionAvatar(
    session: SavedSession,
    modifier: Modifier = Modifier,
    size: Dp = SessionAvatarSize,
) {
    val (container, content) = sessionAvatarColors(session)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clearAndSetSemantics {},
    ) {
        Text(
            text = session.initial,
            color = content,
            maxLines = 1,
            style = if (size < LargeAvatarSize) {
                MaterialTheme.typography.labelLarge
            } else {
                MaterialTheme.typography.titleMedium
            },
        )
    }
}

/** One of the theme's container roles, so several sessions are told apart by more than a letter. */
@Composable
private fun sessionAvatarColors(session: SavedSession): Pair<Color, Color> {
    val scheme = MaterialTheme.colorScheme
    val role = "${session.connection.serverUrl}${session.username}".hashCode().mod(AvatarRoles)
    return when (role) {
        0 -> scheme.primaryContainer to scheme.onPrimaryContainer
        1 -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        else -> scheme.secondaryContainer to scheme.onSecondaryContainer
    }
}

/**
 * The sessions kept on this device. With a [current] session like the account dialog of Google's
 * apps: who is logged in with a way to log out, below it the others, most recently used first; a
 * tap opens one. The bin forgets a session with its credentials, after asking, since logging in
 * to it again needs them.
 */
@Composable
fun SessionsDialog(
    sessions: List<SavedSession>,
    current: SavedSession?,
    switching: SavedSession?,
    error: SessionError?,
    onSessionClick: (SavedSession) -> Unit,
    onRemoveSession: (SavedSession) -> Unit,
    onDismissRequest: () -> Unit,
    onLogoutClick: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var removing by remember { mutableStateOf<SavedSession?>(null) }

    // The last one removed leaves nothing to show
    LaunchedEffect(sessions.isEmpty()) {
        if (sessions.isEmpty()) onDismissRequest()
    }

    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        SessionsDialogContent(
            others = sessions.filterNot { it == current },
            current = current,
            switching = switching,
            error = error,
            onSessionClick = {
                haptic.confirm()
                onSessionClick(it)
            },
            onRemoveClick = {
                haptic.confirm()
                removing = it
            },
            onLogoutClick = onLogoutClick?.let { logout ->
                {
                    haptic.confirm()
                    logout()
                }
            },
            onCloseClick = {
                haptic.confirm()
                onDismissRequest()
            },
        )
    }

    removing?.let { session ->
        RemoveSessionDialog(
            session = session,
            onConfirm = {
                removing = null
                onRemoveSession(session)
            },
            onDismiss = { removing = null },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SessionsDialogContent(
    others: List<SavedSession>,
    current: SavedSession?,
    switching: SavedSession?,
    error: SessionError?,
    onSessionClick: (SavedSession) -> Unit,
    onRemoveClick: (SavedSession) -> Unit,
    onLogoutClick: (() -> Unit)?,
    onCloseClick: () -> Unit,
) {
    ScrollableAlertDialogContent(
        title = { Text(stringResource(Res.string.sessions_title)) },
        confirmButton = {
            TextButton(
                onClick = onCloseClick,
                modifier = Modifier.clickCursor(),
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(Res.string.action_close))
            }
        },
        isScrollableControlledByContent = true,
    ) {
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .dialogListScrollbar(listState),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            if (current != null) {
                val count = if (onLogoutClick != null) 2 else 1
                item {
                    CurrentSessionItem(current, index = 0, count = count, onRemoveClick)
                }
                onLogoutClick?.let { logout ->
                    item {
                        LogoutItem(index = 1, count = count, onClick = logout)
                    }
                }
                if (others.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.sessions_other),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 16.dp, top = 16.dp, bottom = 6.dp)
                                .semantics { heading() },
                        )
                    }
                }
            }
            itemsIndexed(others) { index, session ->
                OtherSessionItem(
                    session = session,
                    isSwitching = session == switching,
                    enabled = switching == null,
                    index = index,
                    count = others.size,
                    onClick = { onSessionClick(session) },
                    onRemoveClick = { onRemoveClick(session) },
                )
            }
            if (error != null) {
                item {
                    Text(
                        text = stringResource(error.message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CurrentSessionItem(
    session: SavedSession,
    index: Int,
    count: Int,
    onRemoveClick: (SavedSession) -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        leadingContent = { SessionAvatar(session, size = LargeAvatarSize) },
        trailingContent = { RemoveButton(enabled = true, onClick = { onRemoveClick(session) }) },
        supportingContent = { SessionHost(session) },
        content = { SessionName(session) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LogoutItem(index: Int, count: Int, onClick: () -> Unit) {
    SegmentedListItem(
        onClick = onClick,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            // As wide as the profile circle above it, so both texts start in line
            Box(Modifier.size(LargeAvatarSize), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_logout),
                    contentDescription = null,
                )
            }
        },
        supportingContent = {
            Text(stringResource(Res.string.settings_server_logout_description))
        },
        content = { Text(stringResource(Res.string.settings_server_logout)) },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OtherSessionItem(
    session: SavedSession,
    isSwitching: Boolean,
    enabled: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
) {
    SegmentedListItem(
        onClick = onClick,
        enabled = enabled,
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(enabled = enabled),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = { SessionAvatar(session, size = LargeAvatarSize) },
        trailingContent = {
            if (isSwitching) {
                LoadingIndicator(modifier = Modifier.size(40.dp))
            } else {
                RemoveButton(enabled = enabled, onClick = onRemoveClick)
            }
        },
        supportingContent = { SessionHost(session) },
        content = { SessionName(session) },
    )
}

@Composable
private fun SessionName(session: SavedSession) {
    Text(session.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
private fun SessionHost(session: SavedSession) {
    // Without a known user the title is the address already
    if (session.name != session.host) {
        Text(session.host, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RemoveButton(enabled: Boolean, onClick: () -> Unit) {
    val description = stringResource(Res.string.sessions_remove)
    HapticTooltipBox(text = description) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.clickCursor(enabled = enabled),
            shapes = IconButtonDefaults.shapes(),
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_rounded_delete),
                contentDescription = description,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RemoveSessionDialog(
    session: SavedSession,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    ScrollableAlertDialog(onDismissRequest = onDismiss) {
        ScrollableAlertDialogContent(
            title = { Text(stringResource(Res.string.sessions_remove_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onConfirm()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        haptic.confirm()
                        onDismiss()
                    },
                    modifier = Modifier.clickCursor(),
                    shapes = ButtonDefaults.shapes(),
                ) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        ) {
            Text(stringResource(Res.string.sessions_remove_message, session.name, session.host))
        }
    }
}

/** The size of a profile circle, which the start screen's logo turns into. */
val SessionAvatarSize = 32.dp

private val LargeAvatarSize = 40.dp

private const val AvatarRoles = 3

@Preview
@Composable
private fun SessionsDialogPreview() {
    GrocyTheme {
        val own = SavedSession(
            ServerConnection.ApiKey("https://grocy.example.com", "key"),
            userId = 1,
            username = "anna",
            displayName = "Anna",
        )
        SessionsDialogContent(
            others = listOf(SavedSession(ServerConnection.Demo(), 1, "demo", "Demo User")),
            current = own,
            switching = null,
            error = null,
            onSessionClick = {},
            onRemoveClick = {},
            onLogoutClick = {},
            onCloseClick = {},
        )
    }
}
