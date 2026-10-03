package com.patrickzedler.grocy.core.design.component

import androidx.annotation.FloatRange
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.MultiContentMeasurePolicy
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.fastMaxBy
import androidx.compose.ui.util.fastMaxOfOrNull
import androidx.compose.ui.util.fastRoundToInt
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.action_more_options
import com.patrickzedler.grocy.core.resources.ic_rounded_more_vert
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * A vertical layout that arranges buttons in a column, supporting dynamic height expansions
 * on press interactions and compressing adjacent neighbors.
 */
@Composable
fun VerticalButtonGroup(
    overflowIndicator: @Composable (VerticalButtonGroupMenuState) -> Unit,
    modifier: Modifier = Modifier,
    @FloatRange(0.0) expandedRatio: Float = VerticalButtonGroupDefaults.ExpandedRatio,
    verticalArrangement: Arrangement.Vertical = VerticalButtonGroupDefaults.VerticalArrangement,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    content: VerticalButtonGroupScope.() -> Unit,
) {
    val defaultAnimationSpec = VerticalButtonGroupDefaults.AnimationSpec
    val scope: VerticalButtonGroupScopeImpl by rememberVerticalButtonGroupScopeState(
        content = content,
        animationSpec = defaultAnimationSpec,
    )
    val menuState = remember { VerticalButtonGroupMenuState() }
    val overflowState = rememberOverflowState()

    val measurePolicy = remember(
        verticalArrangement,
        horizontalAlignment,
        overflowState,
        expandedRatio,
    ) {
        VerticalButtonGroupMeasurePolicy(
            overflowState = overflowState,
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            expandedRatio = expandedRatio,
        )
    }

    Layout(
        contents = listOf(
            { scope.items.fastForEach { it.ButtonGroupContent() } },
            {
                Box {
                    overflowIndicator(menuState)
                    DropdownMenu(
                        expanded = menuState.isShowing,
                        onDismissRequest = { menuState.dismiss() },
                    ) {
                        val total = min(overflowState.totalItemCount, scope.items.size)
                        val start = min(overflowState.visibleItemCount, total)
                        for (i in start until total) {
                            scope.items[i].MenuContent(menuState)
                        }
                    }
                }
            },
        ),
        measurePolicy = measurePolicy,
        modifier = modifier,
    )
}

object VerticalButtonGroupDefaults {
    val ExpandedRatio: Float = 0.15f
    val BetweenSpace: Dp = 8.dp
    val VerticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(BetweenSpace)
    val AnimationSpec: AnimationSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 1400f,
    )

    @Composable
    fun OverflowIndicator(
        menuState: VerticalButtonGroupMenuState,
        modifier: Modifier = Modifier,
        enabled: Boolean = true,
        shape: Shape = IconButtonDefaults.filledShape,
        colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
        interactionSource: MutableInteractionSource? = null,
    ) {
        val contentDescription = stringResource(Res.string.action_more_options)

        HapticTooltipBox(text = contentDescription) {
            FilledIconButton(
                onClick = {
                    if (menuState.isShowing) menuState.dismiss() else menuState.show()
                },
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                interactionSource = interactionSource,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_rounded_more_vert),
                    contentDescription = contentDescription,
                )
            }
        }
    }
}

class VerticalButtonGroupMenuState(
    initialIsShowing: Boolean = false,
) {
    var isShowing: Boolean by mutableStateOf(initialIsShowing)
        private set

    fun dismiss() {
        isShowing = false
    }

    fun show() {
        isShowing = true
    }
}

private class VerticalButtonGroupMeasurePolicy(
    val overflowState: ButtonGroupOverflowState,
    val verticalArrangement: Arrangement.Vertical,
    val horizontalAlignment: Alignment.Horizontal,
    val expandedRatio: Float,
) : MultiContentMeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<List<Measurable>>,
        constraints: Constraints,
    ): MeasureResult {
        val (contentMeasurables, overflowMeasurables) = measurables
        val size = contentMeasurables.size
        overflowState.totalItemCount = size

        if (size == 0) {
            overflowState.visibleItemCount = 0
            return layout(constraints.minWidth, constraints.minHeight) {}
        }

        val arrangementSpacingInt = verticalArrangement.spacing.roundToPx()
        val arrangementSpacingPx = arrangementSpacingInt.toLong()

        var totalWeight = 0f
        var fixedSpace = 0
        var weightChildrenCount = 0
        val childrenConstraints: Array<Constraints?> = arrayOfNulls(size)
        val configs = Array(size) {
            contentMeasurables[it].parentData as? VerticalButtonGroupParentData
        }

        val mainAxisMin = constraints.minHeight
        val mainAxisMax = constraints.maxHeight

        for (i in 0 until size) {
            val child = contentMeasurables[i]
            val weight = child.verticalButtonGroupParentData.weight

            if (weight > 0f) {
                totalWeight += weight
                ++weightChildrenCount
            } else {
                val remaining = mainAxisMax - fixedSpace
                val desiredHeight = child.maxIntrinsicHeight(constraints.maxWidth)
                childrenConstraints[i] = constraints.copy(
                    minHeight = 0,
                    maxHeight = desiredHeight.coerceAtLeast(0),
                )
                val spaceAfterLastNoWeight = min(
                    arrangementSpacingInt,
                    (remaining - desiredHeight).coerceAtLeast(0),
                )
                fixedSpace += desiredHeight + spaceAfterLastNoWeight
            }
        }

        if (weightChildrenCount != 0) {
            val targetSpace = if (mainAxisMax != Constraints.Infinity) mainAxisMax else mainAxisMin
            val arrangementSpacingTotal = arrangementSpacingPx * (weightChildrenCount - 1)
            val remainingToTarget = (targetSpace - fixedSpace - arrangementSpacingTotal)
                .coerceAtLeast(0)

            val weightUnitSpace = remainingToTarget / totalWeight
            var remainder = remainingToTarget
            for (i in 0 until size) {
                val itemWeight = contentMeasurables[i].verticalButtonGroupParentData.weight
                if (itemWeight > 0f) {
                    remainder -= (weightUnitSpace * itemWeight).fastRoundToInt()
                }
            }

            for (i in 0 until size) {
                if (childrenConstraints[i] == null) {
                    val weight = contentMeasurables[i].verticalButtonGroupParentData.weight
                    val remainderUnit = remainder.sign
                    remainder -= remainderUnit
                    val weightedSize = weightUnitSpace * weight
                    val childMainAxisSize = max(0, weightedSize.fastRoundToInt() + remainderUnit)

                    childrenConstraints[i] = constraints.copy(
                        minHeight = if (childMainAxisSize != Constraints.Infinity) {
                            childMainAxisSize
                        } else {
                            0
                        },
                        maxHeight = childMainAxisSize,
                    )
                }
            }
        }

        var remainingSpace = mainAxisMax
        var mainSpace = 0
        var shownItemSpace = 0
        val heights = IntArray(size) {
            (childrenConstraints[it] ?: constraints).maxHeight
        }
        val desiredHeight = heights.sum() + arrangementSpacingInt * (size - 1)
        var lastItem = 0

        val overflowPlaceables = if (desiredHeight <= mainAxisMax) {
            lastItem = size
            mainSpace = desiredHeight
            null
        } else {
            val overflowHeight = overflowMeasurables.fastMaxOfOrNull {
                it.maxIntrinsicHeight(constraints.maxWidth)
            } ?: 0

            remainingSpace -= overflowHeight
            mainSpace += overflowHeight

            while (lastItem < heights.size && heights[lastItem] <= remainingSpace) {
                mainSpace += heights[lastItem]
                shownItemSpace += heights[lastItem]
                remainingSpace -= heights[lastItem++] + arrangementSpacingInt
            }

            mainSpace += arrangementSpacingInt * lastItem
            shownItemSpace += arrangementSpacingInt * lastItem

            val maxOverflowHeight = (remainingSpace + overflowHeight).coerceAtLeast(0)
            val overflowConstraints = constraints.copy(
                minHeight = 0,
                maxHeight = maxOverflowHeight,
            )
            overflowMeasurables.fastMap {
                it.measure(overflowConstraints)
            }
        }

        overflowState.visibleItemCount = lastItem

        if (lastItem > 1) {
            for (index in 0 until lastItem) {
                val animValue = configs[index]?.pressedAnimatable?.value ?: 0f
                if (animValue == 0f) continue
                val actualGrowth: Int

                when (index) {
                    in 1 until lastItem - 1 -> {
                        val prevLimit = configs[index - 1]?.compressionLimit?.toPx() ?: 0f
                        val nextLimit = configs[index + 1]?.compressionLimit?.toPx() ?: 0f
                        val maxGrowth = minOf(
                            expandedRatio * heights[index] / 2f,
                            prevLimit,
                            nextLimit,
                        )
                        val targetGrowth = (animValue * maxGrowth).roundToInt()

                        val growthTop = min(targetGrowth, heights[index - 1])
                        val growthBottom = min(targetGrowth, heights[index + 1])
                        heights[index - 1] -= growthTop
                        heights[index + 1] -= growthBottom
                        actualGrowth = growthTop + growthBottom
                    }

                    0 -> {
                        val nextLimit = configs[1]?.compressionLimit?.toPx() ?: 0f
                        val maxGrowth = min(expandedRatio * heights[index], nextLimit)
                        val targetGrowth = (animValue * maxGrowth).roundToInt()
                        val growthBottom = min(targetGrowth, heights[1])
                        heights[1] -= growthBottom
                        actualGrowth = growthBottom
                    }

                    else -> {
                        val prevLimit = configs[index - 1]?.compressionLimit?.toPx() ?: 0f
                        val maxGrowth = min(expandedRatio * heights[index], prevLimit)
                        val targetGrowth = (animValue * maxGrowth).roundToInt()
                        val growthTop = min(targetGrowth, heights[index - 1])
                        heights[index - 1] -= growthTop
                        actualGrowth = growthTop
                    }
                }

                heights[index] += actualGrowth
            }
        }

        val placeables = ArrayList<Placeable>(lastItem)
        for (index in 0 until lastItem) {
            placeables.add(
                contentMeasurables[index].measure(
                    (childrenConstraints[index] ?: constraints).copy(
                        minHeight = heights[index],
                        maxHeight = heights[index],
                    ),
                ),
            )
        }

        val mainAxisLayoutSize = max(mainSpace.coerceAtLeast(0), mainAxisMin)
        val mainAxisPositions = IntArray(lastItem)
        val arrangeHeights = if (lastItem == heights.size) {
            heights
        } else {
            heights.copyOfRange(0, lastItem)
        }

        with(verticalArrangement) {
            arrange(
                mainAxisLayoutSize,
                arrangeHeights,
                mainAxisPositions,
            )
        }

        val width = max(
            placeables.fastMaxBy { it.width }?.width ?: constraints.minWidth,
            constraints.minWidth,
        )

        return layout(width, mainAxisLayoutSize) {
            for (index in placeables.indices) {
                val alignment = configs[index]?.alignment ?: horizontalAlignment
                val xPosition = alignment.align(placeables[index].width, width, layoutDirection)
                placeables[index].place(x = xPosition, y = mainAxisPositions[index])
            }
            overflowPlaceables?.fastForEach {
                val xPosition = horizontalAlignment.align(it.width, width, layoutDirection)
                it.place(x = xPosition, y = shownItemSpace)
            }
        }
    }
}

sealed interface VerticalButtonGroupScope {
    @Stable
    fun Modifier.weight(
        @FloatRange(from = 0.0, fromInclusive = false) weight: Float,
    ): Modifier

    @Stable
    fun Modifier.animateHeight(interactionSource: InteractionSource): Modifier

    @Stable
    fun Modifier.animateHeight(
        interactionSource: InteractionSource,
        compressionLimit: Dp,
    ): Modifier

    @Stable
    fun Modifier.align(alignment: Alignment.Horizontal): Modifier

    fun clickableItem(
        onClick: () -> Unit,
        label: String,
        icon: (@Composable () -> Unit)? = null,
        weight: Float = Float.NaN,
        enabled: Boolean = true,
    )

    fun customItem(
        buttonGroupContent: @Composable () -> Unit,
        menuContent: @Composable (VerticalButtonGroupMenuState) -> Unit,
    )
}

internal val IntrinsicMeasurable.verticalButtonGroupParentData: VerticalButtonGroupParentData?
    get() = parentData as? VerticalButtonGroupParentData

internal val VerticalButtonGroupParentData?.weight: Float
    get() = this?.weight ?: 0f

internal data class VerticalButtonGroupParentData(
    var weight: Float = 0f,
    var pressedAnimatable: Animatable<Float, AnimationVector1D> = Animatable(0f),
    var alignment: Alignment.Horizontal? = null,
    var compressionLimit: Dp = 0.dp,
)

internal class VerticalButtonGroupElement(
    val weight: Float = 0f,
) : ModifierNodeElement<VerticalButtonGroupNode>() {

    override fun create(): VerticalButtonGroupNode = VerticalButtonGroupNode(weight)

    override fun update(node: VerticalButtonGroupNode) {
        node.weight = weight
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "weight"
        value = weight
        properties["weight"] = weight
    }

    override fun hashCode(): Int = weight.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? VerticalButtonGroupElement ?: return false
        return weight == otherModifier.weight
    }
}

internal class VerticalButtonGroupNode(
    var weight: Float,
) : ParentDataModifierNode, Modifier.Node() {

    override fun Density.modifyParentData(parentData: Any?): Any =
        ((parentData as? VerticalButtonGroupParentData) ?: VerticalButtonGroupParentData()).also {
            it.weight = weight
        }
}

internal class EnlargeOnPressHeightElement(
    val interactionSource: InteractionSource,
    val animationSpec: AnimationSpec<Float>,
    val compressionLimit: Dp? = null,
) : ModifierNodeElement<EnlargeOnPressHeightNode>() {

    override fun create(): EnlargeOnPressHeightNode =
        EnlargeOnPressHeightNode(interactionSource, animationSpec, compressionLimit)

    override fun update(node: EnlargeOnPressHeightNode) {
        if (node.interactionSource != interactionSource) {
            node.interactionSource = interactionSource
            node.launchCollectionJob()
        }
        node.animationSpec = animationSpec
        node.compressionLimit = compressionLimit
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "animateHeight"
        properties["interactionSource"] = interactionSource
        properties["animationSpec"] = animationSpec
        properties["compressionLimit"] = compressionLimit
    }

    override fun hashCode(): Int {
        var result = interactionSource.hashCode()
        result = 31 * result + animationSpec.hashCode()
        result = 31 * result + (compressionLimit?.hashCode() ?: 0)
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? EnlargeOnPressHeightElement ?: return false
        return interactionSource == otherModifier.interactionSource &&
                animationSpec == otherModifier.animationSpec &&
                compressionLimit == otherModifier.compressionLimit
    }
}

internal class EnlargeOnPressHeightNode(
    var interactionSource: InteractionSource,
    var animationSpec: AnimationSpec<Float>,
    var compressionLimit: Dp?,
) : ParentDataModifierNode, Modifier.Node() {

    val pressedAnimatable: Animatable<Float, AnimationVector1D> = Animatable(0f)
    private var collectionJob: Job? = null

    override fun onAttach() {
        launchCollectionJob()
    }

    override fun onDetach() {
        collectionJob?.cancel()
        collectionJob = null
    }

    internal fun launchCollectionJob() {
        collectionJob?.cancel()
        collectionJob = coroutineScope.launch {
            val pressInteractions = mutableListOf<PressInteraction.Press>()
            var animJob: Job? = null
            interactionSource.interactions
                .map { interaction ->
                    when (interaction) {
                        is PressInteraction.Press -> pressInteractions.add(interaction)
                        is PressInteraction.Release -> pressInteractions.remove(interaction.press)
                        is PressInteraction.Cancel -> pressInteractions.remove(interaction.press)
                    }
                    pressInteractions.isNotEmpty()
                }
                .distinctUntilChanged()
                .collectLatest { pressed ->
                    if (pressed) {
                        animJob?.cancel()
                        animJob = coroutineScope.launch {
                            pressedAnimatable.animateTo(1f, animationSpec)
                        }
                    } else {
                        // Lets a short tap finish most of the press animation before releasing
                        waitUntil { pressedAnimatable.value > 0.75f }
                        animJob?.cancel()
                        animJob = coroutineScope.launch {
                            pressedAnimatable.animateTo(0f, animationSpec)
                        }
                    }
                }
        }
    }

    override fun Density.modifyParentData(parentData: Any?): Any {
        val current = (parentData as? VerticalButtonGroupParentData)
            ?: VerticalButtonGroupParentData()
        val resolvedLimit = compressionLimit
            ?: ButtonDefaults.ContentPadding.calculateBottomPadding()
        return VerticalButtonGroupParentData(
            weight = current.weight,
            pressedAnimatable = pressedAnimatable,
            alignment = current.alignment,
            compressionLimit = resolvedLimit,
        )
    }
}

internal interface VerticalButtonGroupItem {
    @Composable
    fun ButtonGroupContent()

    @Composable
    fun MenuContent(state: VerticalButtonGroupMenuState)
}

internal class ClickableVerticalButtonGroupItem(
    private val onClick: () -> Unit,
    private val icon: (@Composable () -> Unit)?,
    private val weight: Float,
    private val animationSpec: AnimationSpec<Float>,
    private val enabled: Boolean,
    private val label: String,
) : VerticalButtonGroupItem {

    @Composable
    override fun ButtonGroupContent() {
        val interactionSource = remember { MutableInteractionSource() }
        val contentPadding = if (icon != null) {
            ButtonDefaults.ButtonWithIconContentPadding
        } else {
            ButtonDefaults.ContentPadding
        }
        val compressionLimit = contentPadding.calculateBottomPadding()

        val modifier = Modifier
            .then(
                EnlargeOnPressHeightElement(
                    interactionSource = interactionSource,
                    animationSpec = animationSpec,
                    compressionLimit = compressionLimit,
                ),
            )
            .then(
                if (!weight.isNaN()) {
                    VerticalButtonGroupElement(weight.coerceAtMost(Float.MAX_VALUE))
                } else {
                    Modifier
                },
            )

        Button(
            onClick = onClick,
            modifier = modifier,
            interactionSource = interactionSource,
            enabled = enabled,
            contentPadding = contentPadding,
        ) {
            icon?.let {
                it()
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            }
            Text(
                text = label,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
            )
        }
    }

    @Composable
    override fun MenuContent(state: VerticalButtonGroupMenuState) {
        DropdownMenuItem(
            enabled = enabled,
            leadingIcon = icon,
            text = { Text(label) },
            onClick = {
                onClick()
                state.dismiss()
            },
        )
    }
}

internal class CustomVerticalButtonGroupItem(
    private val buttonGroupContent: @Composable () -> Unit,
    private val menuContent: @Composable (VerticalButtonGroupMenuState) -> Unit,
) : VerticalButtonGroupItem {

    @Composable
    override fun ButtonGroupContent() = buttonGroupContent()

    @Composable
    override fun MenuContent(state: VerticalButtonGroupMenuState) = menuContent(state)
}

internal class HorizontalAlignElement(
    val alignment: Alignment.Horizontal,
) : ModifierNodeElement<HorizontalAlignNode>() {

    override fun create(): HorizontalAlignNode = HorizontalAlignNode(alignment)

    override fun update(node: HorizontalAlignNode) {
        node.alignment = alignment
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "align"
        value = alignment
        properties["alignment"] = alignment
    }

    override fun hashCode(): Int = alignment.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? HorizontalAlignElement ?: return false
        return alignment == otherModifier.alignment
    }
}

internal class HorizontalAlignNode(
    var alignment: Alignment.Horizontal,
) : ParentDataModifierNode, Modifier.Node() {

    override fun Density.modifyParentData(parentData: Any?): Any =
        ((parentData as? VerticalButtonGroupParentData) ?: VerticalButtonGroupParentData()).also {
            it.alignment = alignment
        }
}

private interface ButtonGroupOverflowState {
    var totalItemCount: Int
    var visibleItemCount: Int
}

@Composable
private fun rememberOverflowState(): ButtonGroupOverflowState =
    rememberSaveable(saver = OverflowStateImpl.Saver) { OverflowStateImpl() }

private const val MAX_WAIT_TIME_MILLIS = 1_000L

private suspend fun waitUntil(condition: () -> Boolean) {
    val initialTimeMillis = withFrameMillis { it }
    while (!condition()) {
        val timeMillis = withFrameMillis { it }
        if (timeMillis - initialTimeMillis > MAX_WAIT_TIME_MILLIS) return
    }
}

private class OverflowStateImpl : ButtonGroupOverflowState {
    override var totalItemCount: Int by mutableIntStateOf(0)
    override var visibleItemCount: Int by mutableIntStateOf(0)

    companion object {
        val Saver: Saver<OverflowStateImpl, *> = Saver(
            save = { listOf(it.totalItemCount, it.visibleItemCount) },
            restore = {
                OverflowStateImpl().apply {
                    totalItemCount = it[0]
                    visibleItemCount = it[1]
                }
            },
        )
    }
}

@Composable
private fun rememberVerticalButtonGroupScopeState(
    content: VerticalButtonGroupScope.() -> Unit,
    animationSpec: AnimationSpec<Float>,
): State<VerticalButtonGroupScopeImpl> {
    val latestContent = rememberUpdatedState(content)
    return remember(animationSpec) {
        derivedStateOf {
            VerticalButtonGroupScopeImpl(animationSpec = animationSpec).apply(latestContent.value)
        }
    }
}

private class VerticalButtonGroupScopeImpl(
    val animationSpec: AnimationSpec<Float>,
) : VerticalButtonGroupScope {

    val items: MutableList<VerticalButtonGroupItem> = mutableListOf()

    override fun clickableItem(
        onClick: () -> Unit,
        label: String,
        icon: (@Composable () -> Unit)?,
        weight: Float,
        enabled: Boolean,
    ) {
        require(weight > 0.0 || weight.isNaN()) {
            "invalid weight $weight; must be greater than zero or Float.NaN"
        }
        items.add(
            ClickableVerticalButtonGroupItem(
                onClick = onClick,
                icon = icon,
                enabled = enabled,
                weight = weight,
                animationSpec = animationSpec,
                label = label,
            ),
        )
    }

    override fun customItem(
        buttonGroupContent: @Composable () -> Unit,
        menuContent: @Composable (VerticalButtonGroupMenuState) -> Unit,
    ) {
        items.add(CustomVerticalButtonGroupItem(buttonGroupContent, menuContent))
    }

    override fun Modifier.weight(weight: Float): Modifier {
        require(weight > 0.0) { "invalid weight $weight; must be greater than zero" }
        return this.then(
            VerticalButtonGroupElement(weight = weight.coerceAtMost(Float.MAX_VALUE)),
        )
    }

    override fun Modifier.animateHeight(interactionSource: InteractionSource): Modifier =
        this.then(
            EnlargeOnPressHeightElement(
                interactionSource = interactionSource,
                animationSpec = animationSpec,
                compressionLimit = null,
            ),
        )

    override fun Modifier.animateHeight(
        interactionSource: InteractionSource,
        compressionLimit: Dp,
    ): Modifier =
        this.then(
            EnlargeOnPressHeightElement(
                interactionSource = interactionSource,
                animationSpec = animationSpec,
                compressionLimit = compressionLimit,
            ),
        )

    override fun Modifier.align(alignment: Alignment.Horizontal): Modifier =
        this.then(HorizontalAlignElement(alignment))
}
