package com.patrickzedler.grocy.feature.settings.impl.server

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialog
import com.patrickzedler.grocy.core.design.component.ScrollableAlertDialogContent
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.dialogListScrollbar
import com.patrickzedler.grocy.core.model.DemoServer
import com.patrickzedler.grocy.core.model.DemoServers
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_close
import com.patrickzedler.grocy.core.resources.settings_server_demo_servers
import com.patrickzedler.grocy.core.resources.settings_server_demo_servers_info
import com.patrickzedler.grocy.feature.settings.impl.general.languageName
import com.patrickzedler.grocy.feature.settings.impl.general.languageNameComparator
import org.jetbrains.compose.resources.stringResource

/** Single choice between Grocy's public demos, one per language. */
@Composable
internal fun DemoServerDialog(
    selectedUrl: String,
    onDemoServerSelected: (DemoServer) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    ScrollableAlertDialog(onDismissRequest = onDismissRequest) {
        DemoServerDialogContent(
            servers = DemoServers,
            selectedUrl = selectedUrl,
            onDemoServerClick = { server ->
                haptic.confirm()
                onDismissRequest()
                if (server.url != selectedUrl) onDemoServerSelected(server)
            },
            onCloseClick = {
                haptic.confirm()
                onDismissRequest()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DemoServerDialogContent(
    servers: List<DemoServer>,
    selectedUrl: String,
    modifier: Modifier = Modifier,
    onDemoServerClick: (DemoServer) -> Unit = {},
    onCloseClick: () -> Unit = {},
) {
    ScrollableAlertDialogContent(
        modifier = modifier,
        title = { Text(stringResource(Res.string.settings_server_demo_servers)) },
        subtitle = { Text(stringResource(Res.string.settings_server_demo_servers_info)) },
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
        val colors = SegmentedListDefaults.colors()
        // Like the language dialog: every language by its own name, in alphabetical order
        val namedServers = remember(servers) {
            servers
                .map { it to languageName(it.languageTag) }
                .sortedWith(compareBy(languageNameComparator()) { (_, name) -> name })
        }

        // The current demo is in view right away, also far down the list
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = namedServers
                .indexOfFirst { (server, _) -> server.url.equals(selectedUrl, ignoreCase = true) }
                .coerceAtLeast(0),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .dialogListScrollbar(listState),
            state = listState,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(
                items = namedServers,
                key = { _, (server, _) -> server.url },
            ) { index, (server, name) ->
                val selected = server.url.equals(selectedUrl, ignoreCase = true)
                SegmentedListItem(
                    selected = selected,
                    onClick = { onDemoServerClick(server) },
                    shapes = ListItemDefaults.segmentedShapes(
                        index = index,
                        count = namedServers.size,
                    ),
                    colors = colors,
                    modifier = Modifier.fillMaxWidth().clickCursor(),
                    verticalAlignment = Alignment.CenterVertically,
                    leadingContent = {
                        LeadingTrailingContentWrapper {
                            // The item handles the click, the radio button only shows the choice
                            RadioButton(
                                selected = selected,
                                modifier = Modifier.clickCursor(),
                                onClick = null,
                            )
                        }
                    },
                    supportingContent = { Text(server.url.removePrefix("https://")) },
                    content = { Text(name) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun DemoServerDialogPreview() {
    GrocyTheme {
        DemoServerDialogContent(
            servers = DemoServers.take(4),
            selectedUrl = DemoServers[1].url,
        )
    }
}
