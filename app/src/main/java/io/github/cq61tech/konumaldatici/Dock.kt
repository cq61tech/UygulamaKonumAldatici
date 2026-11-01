package io.github.cq61tech.konumaldatici

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.roundToInt

data class DockItem(val label: Int, val icon: ImageVector)

private val DockPad = 6.dp

/**
 * Yüzen hap şeklinde gezinme çubuğu.
 * - Dokununca ilgili sekmeye geçer.
 * - Basılı tutup (veya yana sürükleyerek) kaydırınca büyüyen baloncuk parmağı takip eder,
 *   parmak kalkınca en yakın sekmeye oturur.
 */
@Composable
fun FloatingDock(
    items: List<DockItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val n = items.size
    val padPx = with(density) { DockPad.toPx() }

    var widthPx by remember { mutableIntStateOf(0) }
    val slotW = if (widthPx > 0) (widthPx - 2f * padPx) / n else 0f

    var pressed by remember { mutableStateOf(false) }
    var fingerX by remember { mutableFloatStateOf(0f) }
    val currentOnSelect by rememberUpdatedState(onSelect)

    val slotAt: (Float) -> Int = { x ->
        if (slotW <= 0f) 0 else ((x - padPx) / slotW).toInt().coerceIn(0, n - 1)
    }
    val hover = if (pressed) slotAt(fingerX) else selected

    val targetLeft = if (pressed) {
        (fingerX - slotW / 2f).coerceIn(padPx, padPx + slotW * (n - 1))
    } else {
        padPx + selected * slotW
    }
    val left = animateFloatAsState(
        targetValue = targetLeft,
        animationSpec = if (pressed) {
            spring(dampingRatio = 1f, stiffness = Spring.StiffnessHigh)
        } else {
            spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
        },
        label = "dock_left",
    )
    val bubble = animateFloatAsState(
        targetValue = if (pressed) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "dock_bubble",
    )
    val indicatorColor by animateColorAsState(
        targetValue = if (pressed) cs.primaryContainer else cs.secondaryContainer,
        label = "dock_indicator",
    )
    val activeTint = if (pressed) cs.onPrimaryContainer else cs.onSecondaryContainer

    val pill = RoundedCornerShape(36.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .onSizeChanged { widthPx = it.width }
            .pointerInput(slotW, n) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val startX = down.position.x
                    val slop = viewConfiguration.touchSlop
                    var lastX = startX
                    var tapped = false
                    var moved = false

                    // Uzun basma süresi dolmadan: dokunuş mu, sürükleme mi?
                    val timedOut = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                        while (true) {
                            val ev = awaitPointerEvent()
                            val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                            lastX = ch.position.x
                            if (ch.changedToUpIgnoreConsumed() || !ch.pressed) {
                                tapped = true
                                break
                            }
                            if (abs(ch.position.x - startX) > slop) {
                                moved = true
                                break
                            }
                        }
                    } == null

                    if (tapped) {
                        currentOnSelect(slotAt(startX))
                        return@awaitEachGesture
                    }
                    if (!timedOut && !moved) return@awaitEachGesture

                    // Baloncuk modu
                    if (timedOut) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    fingerX = lastX
                    pressed = true
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: break
                        if (ch.changedToUpIgnoreConsumed() || !ch.pressed) break
                        fingerX = ch.position.x
                        ch.consume()
                    }
                    val target = slotAt(fingerX)
                    pressed = false
                    currentOnSelect(target)
                }
            },
    ) {
        // Hap şeklindeki zemin
        Box(
            Modifier
                .matchParentSize()
                .shadow(10.dp, pill, clip = false)
                .background(cs.surfaceContainerHigh, pill)
                .border(1.dp, cs.outlineVariant.copy(alpha = 0.45f), pill)
        )

        // Kayan seçim baloncuğu
        if (slotW > 0f) {
            Box(
                Modifier
                    .offset { IntOffset(left.value.roundToInt(), 0) }
                    .width(with(density) { slotW.toDp() })
                    .fillMaxHeight()
                    .padding(vertical = DockPad)
                    .graphicsLayer {
                        scaleX = bubble.value
                        scaleY = bubble.value
                    }
                    .background(indicatorColor, RoundedCornerShape(28.dp))
            )
        }

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = DockPad)
        ) {
            items.forEachIndexed { i, item ->
                DockSlot(
                    item = item,
                    active = i == hover,
                    activeTint = activeTint,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                )
            }
        }
    }
}

@Composable
private fun DockSlot(item: DockItem, active: Boolean, activeTint: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val tint by animateColorAsState(
        targetValue = if (active) activeTint else cs.onSurfaceVariant,
        label = "slot_tint",
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.14f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "slot_scale",
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        )
        Text(
            text = stringResource(item.label),
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            softWrap = false,
        )
    }
}
