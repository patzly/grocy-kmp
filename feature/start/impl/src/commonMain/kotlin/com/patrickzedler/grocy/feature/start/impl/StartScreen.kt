package com.patrickzedler.grocy.feature.start.impl

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrickzedler.grocy.core.design.GrocyTheme
import com.patrickzedler.grocy.core.design.component.AppBarLogo
import com.patrickzedler.grocy.core.design.component.AppBarMenuItem
import com.patrickzedler.grocy.core.design.component.DemoServerWarning
import com.patrickzedler.grocy.core.design.component.LeadingTrailingContentWrapper
import com.patrickzedler.grocy.core.design.component.ScreenPane
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.SegmentedListScreen
import com.patrickzedler.grocy.core.design.component.SupportingBulletList
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.segmentedGroup
import com.patrickzedler.grocy.core.model.server.ServerConfig
import com.patrickzedler.grocy.core.model.stock.StockSummary
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.app_name
import com.patrickzedler.grocy.core.resources.ic_rounded_database
import com.patrickzedler.grocy.core.resources.ic_rounded_kitchen
import com.patrickzedler.grocy.core.resources.ic_rounded_settings
import com.patrickzedler.grocy.core.resources.masterdata_title
import com.patrickzedler.grocy.core.resources.start_list_join
import com.patrickzedler.grocy.core.resources.start_stock_all_on_lists
import com.patrickzedler.grocy.core.resources.start_stock_below_min
import com.patrickzedler.grocy.core.resources.start_stock_due_soon
import com.patrickzedler.grocy.core.resources.start_stock_expired
import com.patrickzedler.grocy.core.resources.start_stock_in_stock
import com.patrickzedler.grocy.core.resources.start_stock_in_stock_value
import com.patrickzedler.grocy.core.resources.start_stock_one_on_list
import com.patrickzedler.grocy.core.resources.start_stock_overdue
import com.patrickzedler.grocy.core.resources.start_stock_some_on_lists
import com.patrickzedler.grocy.core.resources.start_stock_within_days
import com.patrickzedler.grocy.core.resources.start_summary_intro_end
import com.patrickzedler.grocy.core.resources.start_summary_intro_list
import com.patrickzedler.grocy.core.resources.stock_overview_title
import com.patrickzedler.grocy.core.resources.title_settings
import com.patrickzedler.grocy.core.ui.AppMenuDialogs
import com.patrickzedler.grocy.core.ui.RefreshErrorEffect
import com.patrickzedler.grocy.core.ui.rememberAppMenu
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** The sections of the start screen, in their order, each a group of its own. */
internal enum class StartSection(
    val title: StringResource,
    val icon: DrawableResource,
    /** Hidden while the server turned the feature off, like Grocy's menu. */
    val isAvailable: (ServerConfig) -> Boolean = { true },
) {
    STOCK_OVERVIEW(Res.string.stock_overview_title, Res.drawable.ic_rounded_kitchen, { it.stock }),
    MASTER_DATA(Res.string.masterdata_title, Res.drawable.ic_rounded_database),
}

/** The sections [config] allows, all of them until the configuration is read. */
internal fun availableSections(config: ServerConfig?): List<StartSection> =
    StartSection.entries.filter { config == null || it.isAvailable(config) }

/**
 * The first screen after login; grows with the app's sections (stock overview and master data so
 * far) and leads to the settings in a group of their own below them. On Grocy's public demo
 * ([isDemoServer]) its warning comes first and opens the server settings. Beside the sections' pages
 * ([isBesideDetail]) it is the start [ScreenPane.List] of the layout and its sections a single
 * choice marking the [shownSection]; the settings open as a screen of their own.
 */
@Composable
internal fun StartScreen(
    viewModel: StartViewModel,
    isDemoServer: Boolean,
    isBesideDetail: Boolean,
    shownSection: StartSection?,
    onSectionClick: (StartSection) -> Unit,
    onSettingsClick: () -> Unit,
    onDemoWarningClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val refresh by viewModel.refresh.collectAsStateWithLifecycle()
    val config by viewModel.serverConfig.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // Settings are an item of the list here, not of the menu
    val appMenu = rememberAppMenu(onAboutClick = onAboutClick)
    AppMenuDialogs(appMenu)

    RefreshErrorEffect(
        error = refresh.error,
        hasData = true,
        snackbarHostState = snackbarHostState,
        onErrorShown = viewModel::onErrorShown,
    )

    StartContent(
        sections = availableSections(config),
        isDemoServer = isDemoServer,
        state = state,
        isRefreshing = refresh.isRefreshing,
        snackbarHostState = snackbarHostState,
        onRefresh = viewModel::refresh,
        isBesideDetail = isBesideDetail,
        shownSection = shownSection,
        onSectionClick = onSectionClick,
        onSettingsClick = onSettingsClick,
        onDemoWarningClick = onDemoWarningClick,
        menuItems = appMenu.items,
        modifier = modifier,
    )
}

@Composable
private fun StartContent(
    sections: List<StartSection>,
    isDemoServer: Boolean,
    state: StartUiState?,
    isRefreshing: Boolean,
    snackbarHostState: SnackbarHostState,
    onRefresh: () -> Unit,
    isBesideDetail: Boolean,
    shownSection: StartSection?,
    onSectionClick: (StartSection) -> Unit,
    onSettingsClick: () -> Unit,
    onDemoWarningClick: () -> Unit,
    menuItems: List<AppBarMenuItem>,
    modifier: Modifier = Modifier,
) {
    SegmentedListScreen(
        title = stringResource(Res.string.app_name),
        modifier = modifier,
        // No back button on the first screen, its place shows the logo
        navigationIcon = { AppBarLogo() },
        menuItems = menuItems,
        pane = ScreenPane.List.takeIf { isBesideDetail },
        snackbarHostState = snackbarHostState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
    ) {
        // Above everything, the server settings it opens tell more and switch the demo
        if (isDemoServer) {
            segmentedGroup(key = "demo") {
                DemoServerWarning(onClick = onDemoWarningClick)
            }
        }
        sections.forEach { section ->
            segmentedGroup(key = section.name) {
                SectionItem(
                    section = section,
                    summary = state?.let { sectionSummary(section, it) },
                    index = 0,
                    count = 1,
                    onClick = { onSectionClick(section) },
                    selected = if (isBesideDetail) shownSection == section else null,
                )
            }
        }
        segmentedGroup(key = "settings") {
            SettingsItem(onClick = onSettingsClick)
        }
    }
}

/** Opens the settings, which never show beside the start screen, so a plain item always. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsItem(onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        onClick = {
            haptic.confirm()
            onClick()
        },
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = SegmentedListDefaults.colors(),
        modifier = Modifier.fillMaxWidth().clickCursor(),
        verticalAlignment = Alignment.CenterVertically,
        leadingContent = {
            LeadingTrailingContentWrapper {
                Icon(painterResource(Res.drawable.ic_rounded_settings), contentDescription = null)
            }
        },
        content = { Text(stringResource(Res.string.title_settings)) },
    )
}

/** What a section tells about its data: an introduction and a list, both optional. */
private class SectionSummary(val intro: String?, val bullets: List<String>)

@Composable
private fun sectionSummary(section: StartSection, state: StartUiState): SectionSummary? =
    when (section) {
        StartSection.STOCK_OVERVIEW -> state.stock?.let { stockSummary(it, state) }
        StartSection.MASTER_DATA -> state.masterDataCounts
            .map { (count, number) -> pluralStringResource(count.text, number, number) }
            .takeIf { it.isNotEmpty() }
            ?.let { SectionSummary(intro = joinedList(it), bullets = emptyList()) }
    }

/** [items] as one line of text, joined like the app language joins a list. */
@Composable
private fun joinedList(items: List<String>): String =
    items.reduce { joined, next -> stringResource(Res.string.start_list_join, joined, next) }

/**
 * What is in stock and worth, then like Grocy's messages above its stock overview what is due,
 * overdue, expired and below the min. stock amount; numbers of 0 are left out.
 */
@Composable
private fun stockSummary(summary: StockSummary, state: StartUiState): SectionSummary {
    val count = summary.productCount
    val value = summary.totalValue?.takeIf { count > 0 }
        ?.let { formatCurrency(it, state.currency, state.priceDecimals) }
    val inStock = if (value != null) {
        pluralStringResource(Res.plurals.start_stock_in_stock_value, count, count, value)
    } else {
        pluralStringResource(Res.plurals.start_stock_in_stock, count, count)
    }
    val bullets = buildList {
        if (summary.dueSoonCount > 0) {
            val within = pluralStringResource(
                Res.plurals.start_stock_within_days,
                state.dueSoonDays,
                state.dueSoonDays,
            )
            add(
                pluralStringResource(
                    Res.plurals.start_stock_due_soon,
                    summary.dueSoonCount,
                    summary.dueSoonCount,
                    within,
                ),
            )
        }
        if (summary.overdueCount > 0) {
            add(
                pluralStringResource(
                    Res.plurals.start_stock_overdue,
                    summary.overdueCount,
                    summary.overdueCount,
                ),
            )
        }
        if (summary.expiredCount > 0) {
            add(
                pluralStringResource(
                    Res.plurals.start_stock_expired,
                    summary.expiredCount,
                    summary.expiredCount,
                ),
            )
        }
        if (summary.belowMinStockCount > 0) add(belowMinStock(summary))
    }
    val intro = if (bullets.isEmpty()) {
        stringResource(Res.string.start_summary_intro_end, inStock)
    } else {
        stringResource(Res.string.start_summary_intro_list, inStock)
    }
    return SectionSummary(intro, bullets)
}

/** The products below their min. stock amount and how many of them are on shopping lists. */
@Composable
private fun belowMinStock(summary: StockSummary): String {
    val below = summary.belowMinStockCount
    val onLists = summary.belowMinStockOnShoppingListCount
    val text = pluralStringResource(Res.plurals.start_stock_below_min, below, below)
    val listText = when {
        onLists == 0 -> null
        onLists < below ->
            pluralStringResource(Res.plurals.start_stock_some_on_lists, onLists, onLists)
        below == 1 -> stringResource(Res.string.start_stock_one_on_list)
        else -> stringResource(Res.string.start_stock_all_on_lists)
    }
    return listOfNotNull(text, listText).joinToString(" ")
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SectionItem(
    section: StartSection,
    summary: SectionSummary?,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    selected: Boolean?,
) {
    val haptic = LocalHapticFeedback.current
    val click = {
        haptic.confirm()
        onClick()
    }
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val colors = SegmentedListDefaults.colors()
    val modifier = Modifier.fillMaxWidth().clickCursor()
    val leading = @Composable {
        LeadingTrailingContentWrapper {
            Icon(painterResource(section.icon), contentDescription = null)
        }
    }
    val title = @Composable { Text(stringResource(section.title)) }
    val supporting: (@Composable () -> Unit)? = summary?.let {
        { SupportingBulletList(intro = it.intro, bullets = it.bullets) }
    }
    // Several lines keep the icon at the top like Material's three line items
    val alignment = if (summary != null) Alignment.Top else Alignment.CenterVertically

    // Beside a page a single choice marking it, without page a plain item (no radio semantics)
    if (selected != null) {
        SegmentedListItem(
            selected = selected,
            onClick = click,
            shapes = shapes,
            colors = colors,
            modifier = modifier,
            verticalAlignment = alignment,
            leadingContent = leading,
            supportingContent = supporting,
            content = title,
        )
    } else {
        SegmentedListItem(
            onClick = click,
            shapes = shapes,
            colors = colors,
            modifier = modifier,
            verticalAlignment = alignment,
            leadingContent = leading,
            supportingContent = supporting,
            content = title,
        )
    }
}

@Preview
@Composable
private fun StartScreenPreview() {
    GrocyTheme {
        StartContent(
            sections = StartSection.entries,
            isDemoServer = true,
            state = null,
            isRefreshing = false,
            snackbarHostState = remember { SnackbarHostState() },
            onRefresh = {},
            isBesideDetail = false,
            shownSection = null,
            onSectionClick = {},
            onSettingsClick = {},
            onDemoWarningClick = {},
            menuItems = emptyList(),
        )
    }
}
