package com.patrickzedler.grocy.core.design.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Building blocks for the segmented list layout every screen and dialog uses. */
object SegmentedListDefaults {

    /** Distance between two groups. */
    val GroupSpacing = 16.dp

    /** Padding inside a [SegmentedContainer]. */
    val ContainerPadding = PaddingValues(16.dp)

    /** Colors of segmented list items: surfaceBright on the surfaceContainer background. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun colors(): ListItemColors =
        ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceBright)

    /** Shape of the item at [index] of [count] items in one group. */
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun shape(index: Int, count: Int): Shape =
        ListItemDefaults.segmentedShapes(index = index, count = count).shape
}

/**
 * A group of segmented items with an optional [title] above it, as one item of an
 * [InsetLazyColumn].
 */
fun LazyListScope.segmentedGroup(
    key: Any? = null,
    title: StringResource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    insetItem(key = key) {
        SegmentedGroup(title = title?.let { stringResource(it) }, content = content)
    }
}

/** Items of one group with the segmented gap between them, optionally below a [title]. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SegmentedGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        if (title != null) {
            GroupTitle(text = title)
        }
        content()
    }
}

/** Title of a group, the only content placed directly on the screen background. */
@Composable
fun GroupTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = modifier
            .padding(bottom = 8.dp)
            .semantics { heading() },
    )
}

/**
 * A non-clickable segmented item for content that is no list item, e.g. text fields, buttons or a
 * camera preview. Use `SegmentedListItem` for clickable rows.
 */
@Composable
fun SegmentedContainer(
    index: Int,
    count: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = SegmentedListDefaults.ContainerPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = SegmentedListDefaults.shape(index = index, count = count),
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}
