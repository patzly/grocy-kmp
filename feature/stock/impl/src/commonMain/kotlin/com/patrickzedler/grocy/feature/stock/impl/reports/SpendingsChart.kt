package com.patrickzedler.grocy.feature.stock.impl.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.SegmentedListDefaults
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.rememberChartColors
import com.patrickzedler.grocy.core.design.withTabularNumbers
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** The color of [slot], the theme's chart colors; gray for "Other" (null). */
@Composable
internal fun spendingsColor(slot: Int?): Color {
    val colors = rememberChartColors()
    return slot?.let { colors[it % colors.size] } ?: MaterialTheme.colorScheme.outline
}

/**
 * The slice of the chart at [angle] (degrees clockwise from the top), null outside every
 * slice; the slices follow each other clockwise from the top in [chart]'s order.
 */
internal fun sliceAt(chart: SpendingsChart, angle: Float): Int? {
    val total = chart.total
    if (total <= 0.0) return null
    var start = 0.0
    chart.slices.forEachIndexed { index, slice ->
        val sweep = slice.total / total * FULL_CIRCLE
        if (angle >= start && angle < start + sweep) return index
        start += sweep
    }
    return null
}

/**
 * Grocy's doughnut of the spendings: the slices clockwise from the top with a gap of the
 * surface between them, the total in the middle, or the [selected] slice's [selectedText]. A
 * tap on a slice selects it, again or beside the ring shows the total again; the table below
 * is the accessible view, so the chart is one image to screen readers.
 */
@Composable
internal fun SpendingsDonut(
    chart: SpendingsChart,
    totalText: String,
    totalLabel: String,
    selected: Int?,
    selectedText: Pair<String, String>?,
    description: String,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val colors = chart.slices.map { spendingsColor(it.slot) }
    // The chart lies in a segmented container, its color shows between the slices
    val gapColor = SegmentedListDefaults.containerColor
    Box(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(chart, selected) {
                    detectTapGestures { position ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outer = min(size.width, size.height) / 2f
                        val distance = hypot(position.x - center.x, position.y - center.y)
                        val onRing = distance in outer * (1 - RING_SHARE)..outer
                        // Clockwise from the top, like the slices are drawn
                        val angle = (
                            atan2(position.y - center.y, position.x - center.x) * 180 / PI +
                                QUARTER_CIRCLE
                            ).let { if (it < 0) it + FULL_CIRCLE else it }.toFloat()
                        val slice = if (onRing) sliceAt(chart, angle) else null
                        haptic.confirm()
                        onSelect(slice.takeIf { it != selected })
                    }
                },
        ) {
            val diameter = min(size.width, size.height)
            val ring = diameter / 2f * RING_SHARE
            val arcSize = Size(diameter - ring, diameter - ring)
            val topLeft = Offset(
                (size.width - arcSize.width) / 2f,
                (size.height - arcSize.height) / 2f,
            )
            val total = chart.total
            if (total <= 0.0) return@Canvas
            var start = -QUARTER_CIRCLE.toFloat()
            chart.slices.forEachIndexed { index, slice ->
                val sweep = (slice.total / total * FULL_CIRCLE).toFloat()
                val isSelected = selected == null || selected == index
                drawArc(
                    color = colors[index].copy(alpha = if (isSelected) 1f else DIMMED_ALPHA),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = ring),
                )
                start += sweep
            }
            // The surface between the slices, 2dp wide across the ring
            if (chart.slices.size > 1) {
                var edge = -QUARTER_CIRCLE
                val center = Offset(size.width / 2f, size.height / 2f)
                val inner = diameter / 2f - ring
                val outer = diameter / 2f
                chart.slices.forEach { slice ->
                    val radians = edge * PI / 180
                    val direction = Offset(cos(radians).toFloat(), sin(radians).toFloat())
                    drawLine(
                        color = gapColor,
                        start = center + direction * (inner - 1f),
                        end = center + direction * (outer + 1f),
                        strokeWidth = GAP.toPx(),
                    )
                    edge += slice.total / total * FULL_CIRCLE
                }
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val (headline, label) = selectedText ?: (totalText to totalLabel)
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineSmall.withTabularNumbers(),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val FULL_CIRCLE = 360.0
private const val QUARTER_CIRCLE = 90.0

// The ring's width as share of the radius
private const val RING_SHARE = 0.32f

// What the other slices keep of their color while one is selected
private const val DIMMED_ALPHA = 0.35f

private val GAP = 2.dp
