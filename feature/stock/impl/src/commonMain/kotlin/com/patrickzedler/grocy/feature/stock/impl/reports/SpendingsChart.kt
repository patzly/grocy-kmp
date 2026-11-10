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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.design.component.rememberEqualToneChartColors
import com.patrickzedler.grocy.core.design.withTabularNumbers
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

/** The color of [slot], one tone for all so no slice looks more important; gray for "Other". */
@Composable
internal fun spendingsColor(slot: Int?): Color {
    val colors = rememberEqualToneChartColors()
    return slot?.let { colors.series[it % colors.series.size] } ?: colors.other
}

/**
 * How many degrees each slice of [totals] covers: its share, but at least [MIN_SLICE_SWEEP], so a
 * slice of a few cents keeps its rounded shape and stays tappable. The others give way evenly.
 */
internal fun sliceSweeps(totals: List<Double>): List<Float> {
    var enlarged = emptySet<Int>()
    while (true) {
        val rest = totals.indices.filter { it !in enlarged }
        val restTotal = rest.sumOf { totals[it] }
        val available = FULL_CIRCLE - enlarged.size * MIN_SLICE_SWEEP
        val tooSmall = rest.filter { totals[it] / restTotal * available < MIN_SLICE_SWEEP }
        if (tooSmall.isEmpty()) {
            return totals.indices.map { index ->
                if (index in enlarged) {
                    MIN_SLICE_SWEEP.toFloat()
                } else {
                    (totals[index] / restTotal * available).toFloat()
                }
            }
        }
        enlarged = enlarged + tooSmall
    }
}

/**
 * The slice of the chart at [angle] (degrees clockwise from the top), null outside every
 * slice; the slices follow each other clockwise from the top in [chart]'s order.
 */
internal fun sliceAt(chart: SpendingsChart, angle: Float): Int? {
    if (chart.total <= 0.0) return null
    var start = 0f
    sliceSweeps(chart.slices.map { it.total }).forEachIndexed { index, sweep ->
        if (angle >= start && angle < start + sweep) return index
        start += sweep
    }
    return null
}

/**
 * The corner radius a slice of [sweep] radians can have, at most [wanted]: half of the ring and
 * what still fits between its two sides at the inner and the outer edge.
 */
internal fun sliceCornerRadius(
    wanted: Float,
    outer: Float,
    inner: Float,
    gap: Float,
    sweep: Float,
): Float {
    val spread = sin(min(sweep / 2f, (PI / 2).toFloat()))
    val atOuter = (spread * outer - gap / 2f) / (1f + spread)
    val atInner = if (spread < 1f) (spread * inner - gap / 2f) / (1f - spread) else wanted
    return minOf(wanted, (outer - inner) / 2f, atOuter, atInner).coerceAtLeast(0f)
}

/** Part of a ring from [start] over [sweep] radians, its sides [gap] apart from the next part. */
private fun slicePath(
    center: Offset,
    outer: Float,
    inner: Float,
    gap: Float,
    wantedCorner: Float,
    start: Float,
    sweep: Float,
): Path {
    val corner = sliceCornerRadius(wantedCorner, outer, inner, gap, sweep)
    val end = start + sweep
    val outerCenter = outer - corner
    val innerCenter = inner + corner
    // How far the corners' circles lie inside the slice's two sides
    val outerAngle = asin((corner + gap / 2f) / outerCenter).coerceAtMost(sweep / 2f)
    val innerAngle = asin((corner + gap / 2f) / innerCenter).coerceAtMost(sweep / 2f)
    val quarter = (PI / 2).toFloat()
    val half = PI.toFloat()

    fun at(angle: Float, radius: Float) =
        center + Offset(cos(angle) * radius, sin(angle) * radius)

    fun Path.arc(around: Offset, radius: Float, from: Float, over: Float) {
        arcToRad(Rect(around, radius), from, over, forceMoveTo = false)
    }

    return Path().apply {
        moveTo(at(start + outerAngle, outer).x, at(start + outerAngle, outer).y)
        arc(center, outer, start + outerAngle, sweep - 2f * outerAngle)
        arc(at(end - outerAngle, outerCenter), corner, end - outerAngle, quarter + outerAngle)
        arc(at(end - innerAngle, innerCenter), corner, end + quarter, quarter - innerAngle)
        arc(center, inner, end - innerAngle, -(sweep - 2f * innerAngle))
        arc(
            at(start + innerAngle, innerCenter),
            corner,
            start + innerAngle + half,
            quarter - innerAngle,
        )
        arc(at(start + outerAngle, outerCenter), corner, start - quarter, quarter + outerAngle)
        close()
    }
}

/**
 * Grocy's doughnut of the spendings: rounded slices clockwise from the top with a gap between
 * them, the total in the middle, or the [selected] slice's [selectedText]. A
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
    val sweeps = remember(chart) { sliceSweeps(chart.slices.map { it.total }) }
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
            if (chart.total <= 0.0) return@Canvas
            val center = Offset(size.width / 2f, size.height / 2f)
            val outer = min(size.width, size.height) / 2f
            val ring = outer * RING_SHARE
            fun colorOf(index: Int) =
                colors[index].copy(alpha = if (selected in setOf(null, index)) 1f else DIMMED_ALPHA)
            // A single slice is the whole ring, it has no sides to round
            if (chart.slices.size == 1) {
                drawCircle(colorOf(0), outer - ring / 2f, center, style = Stroke(width = ring))
                return@Canvas
            }
            var start = (-QUARTER_CIRCLE * PI / HALF_CIRCLE).toFloat()
            sweeps.forEachIndexed { index, degrees ->
                val sweep = (degrees * PI / HALF_CIRCLE).toFloat()
                drawPath(
                    path = slicePath(
                        center,
                        outer,
                        outer - ring,
                        GAP.toPx(),
                        CORNER.toPx(),
                        start,
                        sweep,
                    ),
                    color = colorOf(index),
                )
                start += sweep
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
private const val HALF_CIRCLE = 180.0
private const val QUARTER_CIRCLE = 90.0

// The narrowest slice that still fits both sides' gap on a small ring
private const val MIN_SLICE_SWEEP = 6.0

// The ring's width as share of the radius
private const val RING_SHARE = 0.32f

// What the other slices keep of their color while one is selected
private const val DIMMED_ALPHA = 0.35f

private val GAP = 4.dp
// Material's extra small shape
private val CORNER = 4.dp
