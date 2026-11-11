package com.patrickzedler.grocy.feature.start.impl

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.patrickzedler.grocy.core.design.component.GrocyLogo
import com.patrickzedler.grocy.core.design.component.clickCursor
import com.patrickzedler.grocy.core.design.component.confirm
import com.patrickzedler.grocy.core.model.SavedSession
import com.patrickzedler.grocy.core.resources.Res
import com.patrickzedler.grocy.core.resources.sessions_open
import com.patrickzedler.grocy.core.resources.sessions_profile
import com.patrickzedler.grocy.core.resources.sessions_switch_next
import com.patrickzedler.grocy.core.ui.sessions.SessionAvatar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * How far a touch has moved a profile circle: a slow start of the finger's way, so the circle
 * only gives way a little, up to [distance]; never upwards.
 */
internal fun swipeOffsetOf(dragged: Float, distance: Float): Float =
    (dragged * SWIPE_RESISTANCE).coerceIn(0f, distance)

/** A swipe released past half of the way goes to the next session, otherwise it falls back. */
internal fun swipeCommits(offset: Float, distance: Float): Boolean =
    distance > 0f && offset >= distance * SWIPE_COMMIT_FRACTION

/**
 * The logo of the start screen, which turns into the profile circle of [session] while pressed,
 * hovered with a mouse and while its dialog is open ([isDialogOpen]). A tap opens the dialog
 * ([onClick]) once the circle is there. Like the account particle of Google's apps a swipe down
 * by touch moves the circle away and brings the one of [next] from above ([onSwipeTo]).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SessionLogoButton(
    session: SavedSession?,
    next: SavedSession?,
    isDialogOpen: Boolean,
    onClick: () -> Unit,
    onSwipeTo: (SavedSession) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isDragging by remember { mutableStateOf(false) }
    // Counted up by every swipe: the circle stays for a moment afterwards, to be read
    var swipes by remember { mutableIntStateOf(0) }
    var isHolding by remember { mutableStateOf(false) }
    val offset = remember { Animatable(0f) }
    val distance = with(LocalDensity.current) { SwipeDistance.toPx() }
    val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val currentNext by rememberUpdatedState(next)
    val currentOnSwipeTo by rememberUpdatedState(onSwipeTo)

    LaunchedEffect(swipes) {
        if (swipes == 0) return@LaunchedEffect
        isHolding = true
        delay(PROFILE_HOLD_MILLIS)
        isHolding = false
    }
    // Another session shown (chosen, opened or failed): its circle is back in the middle
    LaunchedEffect(session?.connection) {
        if (!isDragging && offset.value != 0f) offset.snapTo(0f)
    }

    val showsProfile = session != null &&
        (isPressed || isHovered || isDialogOpen || isDragging || isHolding)
    val transition = updateTransition(showsProfile, label = "profile")
    val logoScale by transition.animateFloat({ spatial }, label = "logo scale") {
        if (it) HIDDEN_SCALE else 1f
    }
    val logoAlpha by transition.animateFloat({ effects }, label = "logo alpha") {
        if (it) 0f else 1f
    }
    val profileScale by transition.animateFloat({ spatial }, label = "profile scale") {
        if (it) 1f else HIDDEN_SCALE
    }
    val profileAlpha by transition.animateFloat({ effects }, label = "profile alpha") {
        if (it) 1f else 0f
    }

    val openLabel = stringResource(Res.string.sessions_open)
    val description = session?.let { stringResource(Res.string.sessions_profile, it.name) }
    val nextLabel = stringResource(Res.string.sessions_switch_next)
    val open = {
        haptic.confirm()
        // At once: the open dialog keeps the circle, which grows in behind its scrim
        onClick()
    }
    val outline = Modifier.border(
        width = ProfileOutlineWidth,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = CircleShape,
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .padding(start = LogoButtonStartPadding)
            .size(LogoButtonSize)
            .semantics {
                description?.let { contentDescription = it }
                currentNext?.let { target ->
                    customActions = listOf(
                        CustomAccessibilityAction(nextLabel) {
                            currentOnSwipeTo(target)
                            true
                        },
                    )
                }
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // A mouse hovers and clicks, only fingers swipe
                    if (down.type != PointerType.Touch) return@awaitEachGesture
                    var dragged = 0f
                    val start = awaitVerticalTouchSlopOrCancellation(down.id) { change, over ->
                        change.consume()
                        dragged = over
                    } ?: return@awaitEachGesture
                    isDragging = true
                    var committed = false
                    try {
                        verticalDrag(start.id) { change ->
                            dragged += change.positionChange().y
                            change.consume()
                            val moved = swipeOffsetOf(dragged, distance)
                            val commits = currentNext != null && swipeCommits(moved, distance)
                            if (commits != committed) {
                                committed = commits
                                haptic.performHapticFeedback(
                                    if (commits) {
                                        HapticFeedbackType.GestureThresholdActivate
                                    } else {
                                        HapticFeedbackType.SegmentFrequentTick
                                    },
                                )
                            }
                            scope.launch { offset.snapTo(moved) }
                        }
                    } finally {
                        isDragging = false
                        swipes++
                    }
                    val target = currentNext.takeIf { committed }
                    scope.launch {
                        if (target != null) {
                            offset.animateTo(distance, spatial)
                            haptic.confirm()
                            currentOnSwipeTo(target)
                        } else {
                            offset.animateTo(0f, spatial)
                        }
                    }
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = LogoButtonSize / 2),
                enabled = session != null,
                role = Role.Button,
                onClickLabel = openLabel,
                onClick = open,
            )
            .clickCursor(enabled = session != null),
    ) {
        GrocyLogo(
            modifier = Modifier.graphicsLayer {
                scaleX = logoScale
                scaleY = logoScale
                alpha = logoAlpha
            },
        )
        if (session != null) {
            // Above the current one and on its way down, so the two follow each other
            next?.let {
                SessionAvatar(
                    session = it,
                    modifier = Modifier.graphicsLayer {
                        val progress = (offset.value / distance).coerceIn(0f, 1f)
                        translationY = offset.value - distance
                        alpha = profileAlpha * progress
                        scaleX = profileScale
                        scaleY = profileScale
                    }.then(outline),
                )
            }
            SessionAvatar(
                session = session,
                modifier = Modifier.graphicsLayer {
                    val progress = (offset.value / distance).coerceIn(0f, 1f)
                    translationY = offset.value
                    alpha = profileAlpha * (1f - progress)
                    scaleX = profileScale
                    scaleY = profileScale
                }.then(outline),
            )
        }
    }
}

private const val SWIPE_RESISTANCE = 0.6f
private const val SWIPE_COMMIT_FRACTION = 0.5f
private const val HIDDEN_SCALE = 0.5f

private const val PROFILE_HOLD_MILLIS = 1_400L

private val SwipeDistance = 40.dp
private val ProfileOutlineWidth = 1.dp
private val LogoButtonSize = 48.dp

// Keeps the logo where it was without this button around it: 16.dp, it is 30.dp wide
private val LogoButtonStartPadding = 7.dp
