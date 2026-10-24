package com.patrickzedler.grocy.feature.masterdata.impl.overview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.chartColors
import com.patrickzedler.grocy.core.model.text.formatCurrency
import com.patrickzedler.grocy.core.model.text.formatDate
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.product_overview_other_stores
import com.patrickzedler.grocy.core.resources.product_overview_price_point
import com.patrickzedler.grocy.core.resources.product_overview_trend
import com.patrickzedler.grocy.core.resources.product_overview_unknown_store
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import org.jetbrains.compose.resources.stringResource

/**
 * Grocy's price history as line chart: one line per store, prices from zero up, the dotted
 * linear trend over all of them. Tapping, dragging, hovering or the arrow keys select a price,
 * which shows date, store and price; screen readers get every price as text.
 */
@Composable
internal fun PriceHistoryChart(
    history: PriceHistory,
    currency: String,
    priceDecimals: Int,
    modifier: Modifier = Modifier,
) {
    val colors = chartColors
    val unknownStore = stringResource(Res.string.product_overview_unknown_store)
    val otherStores = stringResource(Res.string.product_overview_other_stores)
    val storeNames = history.series.map {
        when (val store = it.store) {
            is SeriesStore.Named -> store.name
            SeriesStore.Unknown -> unknownStore
            SeriesStore.Others -> otherStores
        }
    }
    val points = remember(history) { chartPoints(history) }
    val pointFormat = stringResource(Res.string.product_overview_price_point)
    fun describe(point: ChartPoint) = pointFormat.fill(
        formatDate(point.point.epochDay),
        storeNames[point.seriesIndex],
        formatCurrency(point.point.price, currency, priceDecimals),
    )

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PlotArea(
            history = history,
            points = points,
            colors = colors,
            currency = currency,
            priceDecimals = priceDecimals,
            describe = ::describe,
        )
        // One line names itself through the tooltip, more need a key; the dotted trend always
        if (history.series.size > 1 || history.trend != null) {
            Legend(
                names = storeNames,
                colors = history.series.map { colors[it.colorIndex] },
                showTrend = history.trend != null,
            )
        }
    }
}

@Composable
private fun PlotArea(
    history: PriceHistory,
    points: List<ChartPoint>,
    colors: List<Color>,
    currency: String,
    priceDecimals: Int,
    describe: (ChartPoint) -> String,
) {
    val haptic = LocalHapticFeedback.current
    val layoutDirection = LocalLayoutDirection.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
        .copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val tooltipStyle = MaterialTheme.typography.bodySmall
        .copy(color = MaterialTheme.colorScheme.inverseOnSurface)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val trendColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val tooltipColor = MaterialTheme.colorScheme.inverseSurface

    var selected by rememberSaveable(history) { mutableStateOf<Int?>(null) }
    val holder = remember { GeometryHolder() }
    fun select(index: Int?) {
        if (index != null && index != selected) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
        selected = index
    }
    val description = points.joinToString("; ") { describe(it) }
    val selectedText = selected?.let { points.getOrNull(it) }?.let(describe)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(PlotHeight)
            .semantics {
                contentDescription = description
                if (selectedText != null) stateDescription = selectedText
            }
            // Arrow keys walk through the prices for keyboards
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || points.isEmpty()) return@onKeyEvent false
                val forward = when (event.key) {
                    Key.DirectionRight -> layoutDirection == LayoutDirection.Ltr
                    Key.DirectionLeft -> layoutDirection == LayoutDirection.Rtl
                    else -> return@onKeyEvent false
                }
                val current = selected ?: if (forward) -1 else points.size
                select((current + if (forward) 1 else -1).coerceIn(points.indices))
                true
            }
            .focusable()
            .pointerInput(points) {
                detectTapGestures { offset ->
                    select(holder.geometry?.nearest(points, offset, TouchRadius.toPx()))
                }
            }
            // Only sideways, so the dialog still scrolls vertically over the chart
            .pointerInput(points) {
                detectHorizontalDragGestures(
                    onDragStart = { select(holder.geometry?.nearestByDay(points, it.x)) },
                ) { change, _ ->
                    select(holder.geometry?.nearestByDay(points, change.position.x))
                }
            }
            // A mouse shows the price below it without clicking
            .pointerInput(points) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        when (event.type) {
                            PointerEventType.Move -> event.changes
                                .firstOrNull { it.type == PointerType.Mouse }
                                ?.let {
                                    selected =
                                        holder.geometry?.nearestByDay(points, it.position.x)
                                }

                            PointerEventType.Exit -> if (
                                event.changes.any { it.type == PointerType.Mouse }
                            ) {
                                selected = null
                            }
                        }
                    }
                }
            },
    ) {
        val maxPrice = maxOf(
            points.maxOfOrNull { it.point.price } ?: 0.0,
            history.trend?.let { maxOf(it.startPrice, it.endPrice) } ?: 0.0,
        )
        val axis = priceAxis(maxPrice)
        val labels = axis.ticks.map {
            textMeasurer.measure(formatCurrency(it, currency, priceDecimals), labelStyle)
        }
        val firstDay = points.minOf { it.point.epochDay }
        val lastDay = points.maxOf { it.point.epochDay }
        val dateLabels = listOf(firstDay, lastDay).distinct()
            .map { textMeasurer.measure(formatDate(it), labelStyle) }
        val labelGap = 8.dp.toPx()
        val labelWidth = labels.maxOf { it.size.width }.toFloat()
        val dateHeight = dateLabels.maxOf { it.size.height }.toFloat()
        val inset = PointRadius.toPx() + RingWidth.toPx()
        val plot = ChartGeometry(
            left = labelWidth + labelGap + inset,
            top = labels.first().size.height / 2f,
            right = size.width - inset,
            bottom = size.height - dateHeight - labelGap,
            // A single day gets room on both sides
            firstDay = if (firstDay == lastDay) firstDay - 1 else firstDay,
            lastDay = if (firstDay == lastDay) lastDay + 1 else lastDay,
            maxPrice = axis.max,
            isRtl = layoutDirection == LayoutDirection.Rtl,
        )
        holder.geometry = plot

        drawGrid(plot, axis, labels, labelWidth, gridColor)
        drawDateLabels(plot, dateLabels, firstDay, lastDay)
        history.trend?.let { trend ->
            drawLine(
                color = trendColor,
                start = plot.offset(trend.startDay, trend.startPrice),
                end = plot.offset(trend.endDay, trend.endPrice),
                strokeWidth = LineWidth.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(0f, TrendDotGap.toPx()),
                ),
            )
        }
        history.series.forEach { series ->
            drawSeries(plot, series, colors[series.colorIndex], surface)
        }
        selected?.let { points.getOrNull(it) }?.let { point ->
            val color = colors[history.series[point.seriesIndex].colorIndex]
            drawSelection(plot, point, color, surface, gridColor)
            drawTooltip(
                anchor = plot.offset(point.point.epochDay, point.point.price),
                text = textMeasurer.measure(describe(point), tooltipStyle),
                color = tooltipColor,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(names: List<String>, colors: List<Color>, showTrend: Boolean) {
    val trendColor = MaterialTheme.colorScheme.onSurfaceVariant
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        names.forEachIndexed { index, name ->
            LegendItem(text = name) { drawLegendLine(colors[index], dotted = false) }
        }
        if (showTrend) {
            LegendItem(text = stringResource(Res.string.product_overview_trend)) {
                drawLegendLine(trendColor, dotted = true)
            }
        }
    }
}

@Composable
private fun LegendItem(text: String, swatch: DrawScope.() -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(Modifier.size(width = 16.dp, height = 8.dp), onDraw = swatch)
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun DrawScope.drawLegendLine(color: Color, dotted: Boolean) {
    val y = size.height / 2
    drawLine(
        color = color,
        start = Offset(LineWidth.toPx(), y),
        end = Offset(size.width - LineWidth.toPx(), y),
        strokeWidth = LineWidth.toPx(),
        cap = StrokeCap.Round,
        pathEffect = if (dotted) {
            PathEffect.dashPathEffect(floatArrayOf(0f, TrendDotGap.toPx()))
        } else {
            null
        },
    )
}

private fun DrawScope.drawGrid(
    plot: ChartGeometry,
    axis: PriceAxis,
    labels: List<TextLayoutResult>,
    labelWidth: Float,
    color: Color,
) {
    axis.ticks.forEachIndexed { index, price ->
        val y = plot.y(price)
        drawLine(
            color = color,
            start = Offset(plot.left, y),
            end = Offset(plot.right, y),
            strokeWidth = GridWidth.toPx(),
        )
        val label = labels[index]
        // Prices at the start, right aligned towards the plot
        val x = if (plot.isRtl) size.width - labelWidth else labelWidth - label.size.width
        drawText(label, topLeft = Offset(x, y - label.size.height / 2f))
    }
}

private fun DrawScope.drawDateLabels(
    plot: ChartGeometry,
    labels: List<TextLayoutResult>,
    firstDay: Long,
    lastDay: Long,
) {
    val top = size.height - labels.first().size.height
    if (firstDay == lastDay) {
        val label = labels.single()
        val center = plot.x(firstDay)
        drawText(label, topLeft = Offset(center - label.size.width / 2f, top))
        return
    }
    // The first date below the start of the plot, the last one below its end
    val (startLabel, endLabel) = if (plot.isRtl) labels[1] to labels[0] else labels[0] to labels[1]
    drawText(startLabel, topLeft = Offset(plot.left, top))
    drawText(endLabel, topLeft = Offset(plot.right - endLabel.size.width, top))
}

private fun DrawScope.drawSeries(
    plot: ChartGeometry,
    series: PriceSeries,
    color: Color,
    surface: Color,
) {
    val offsets = series.points.map { plot.offset(it.epochDay, it.price) }
    if (offsets.size > 1) {
        val path = Path().apply {
            moveTo(offsets.first().x, offsets.first().y)
            offsets.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = LineWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
    // A ring of the surface keeps crossing points apart
    offsets.forEach {
        drawCircle(surface, radius = PointRadius.toPx() + RingWidth.toPx(), center = it)
        drawCircle(color, radius = PointRadius.toPx(), center = it)
    }
}

private fun DrawScope.drawSelection(
    plot: ChartGeometry,
    point: ChartPoint,
    color: Color,
    surface: Color,
    lineColor: Color,
) {
    val center = plot.offset(point.point.epochDay, point.point.price)
    drawLine(
        color = lineColor,
        start = Offset(center.x, plot.top),
        end = Offset(center.x, plot.bottom),
        strokeWidth = LineWidth.toPx(),
    )
    drawCircle(surface, radius = SelectedRadius.toPx() + RingWidth.toPx(), center = center)
    drawCircle(color, radius = SelectedRadius.toPx(), center = center)
}

private fun DrawScope.drawTooltip(
    anchor: Offset,
    text: TextLayoutResult,
    color: Color,
) {
    val padding = Offset(8.dp.toPx(), 4.dp.toPx())
    val box = Size(
        text.size.width + padding.x * 2,
        text.size.height + padding.y * 2,
    )
    val gap = SelectedRadius.toPx() + 8.dp.toPx()
    // Above the price, below it when there is no room, always inside the chart
    val above = anchor.y - gap - box.height
    val top = if (above >= 0) above else anchor.y + gap
    val left = (anchor.x - box.width / 2).coerceIn(0f, maxOf(0f, size.width - box.width))
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top.coerceAtMost(size.height - box.height)),
        size = box,
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    drawText(
        text,
        topLeft = Offset(left + padding.x, top.coerceAtMost(size.height - box.height) + padding.y),
    )
}

/** A price of the chart with its series, in the order of the days. */
internal data class ChartPoint(val seriesIndex: Int, val point: PricePoint)

internal fun chartPoints(history: PriceHistory): List<ChartPoint> = history.series
    .flatMapIndexed { index, series -> series.points.map { ChartPoint(index, it) } }
    .sortedWith(compareBy({ it.point.epochDay }, { it.seriesIndex }))

/** Ticks of the price axis from zero, in round steps like 0.5, 1, 2 or 2.5. */
internal data class PriceAxis(val max: Double, val ticks: List<Double>)

internal fun priceAxis(maxPrice: Double, tickCount: Int = 4): PriceAxis {
    if (maxPrice <= 0) return PriceAxis(1.0, List(tickCount + 1) { it / tickCount.toDouble() })
    val rawStep = maxPrice / tickCount
    val magnitude = 10.0.pow(floor(log10(rawStep)))
    val step = NiceSteps.first { it * magnitude >= rawStep * (1 - EPSILON) } * magnitude
    val steps = ceil(maxPrice / step - EPSILON).toInt().coerceAtLeast(1)
    return PriceAxis(step * steps, List(steps + 1) { it * step })
}

private val NiceSteps = listOf(1.0, 2.0, 2.5, 5.0, 10.0)
private const val EPSILON = 1e-9

/** Where days and prices lie in the plot, from the chart's last drawing. */
private class ChartGeometry(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val firstDay: Long,
    val lastDay: Long,
    val maxPrice: Double,
    val isRtl: Boolean,
) {
    fun x(day: Long): Float {
        val fraction = (day - firstDay).toFloat() / (lastDay - firstDay)
        return if (isRtl) right - fraction * (right - left) else left + fraction * (right - left)
    }

    fun y(price: Double): Float = bottom - (price / maxPrice).toFloat() * (bottom - top)

    fun offset(day: Long, price: Double) = Offset(x(day), y(price))

    fun nearest(points: List<ChartPoint>, position: Offset, maxDistance: Float): Int? {
        val index = points.indices.minByOrNull { distance(points[it], position) } ?: return null
        return index.takeIf { distance(points[it], position) <= maxDistance }
    }

    fun nearestByDay(points: List<ChartPoint>, x: Float): Int? =
        points.indices.minByOrNull { abs(x(points[it].point.epochDay) - x) }

    private fun distance(point: ChartPoint, position: Offset) =
        (offset(point.point.epochDay, point.point.price) - position).getDistance()
}

/** The geometry of the last drawing for the input handlers, which run outside of it. */
private class GeometryHolder {
    var geometry: ChartGeometry? = null
}

private val PlotHeight = 200.dp
private val LineWidth = 2.dp
private val GridWidth = 1.dp
private val PointRadius = 4.dp
private val SelectedRadius = 6.dp
private val RingWidth = 2.dp
private val TrendDotGap = 6.dp
private val TouchRadius = 24.dp
