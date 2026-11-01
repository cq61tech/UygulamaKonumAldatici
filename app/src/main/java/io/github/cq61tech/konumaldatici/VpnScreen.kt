package io.github.cq61tech.konumaldatici

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class VpnState { Off, Connecting, On }

@Composable
fun VpnScreen(modifier: Modifier = Modifier) {
    var state by remember { mutableStateOf(VpnState.Off) }
    var provider by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Appear(0) {
            ConnectButton(state) {
                when (state) {
                    VpnState.Off -> {
                        state = VpnState.Connecting
                        scope.launch {
                            delay(1700)
                            if (state == VpnState.Connecting) state = VpnState.On
                        }
                    }
                    VpnState.Connecting, VpnState.On -> state = VpnState.Off
                }
            }
        }

        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "status_text",
        ) { s ->
            Text(
                text = stringResource(
                    when (s) {
                        VpnState.Off -> R.string.vpn_state_off
                        VpnState.Connecting -> R.string.vpn_state_connecting
                        VpnState.On -> R.string.vpn_state_on
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }

        Appear(1) {
            SectionCard(stringResource(R.string.vpn_provider)) {
                val providers = listOf(
                    R.string.provider_proton,
                    R.string.provider_custom,
                    R.string.provider_own,
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    providers.forEachIndexed { i, res ->
                        FilterChip(
                            selected = provider == i,
                            onClick = { provider = i },
                            label = { Text(stringResource(res)) },
                        )
                    }
                }
            }
        }

        Appear(2) {
            SectionCard(stringResource(R.string.nav_vpn)) {
                InfoRow(stringResource(R.string.vpn_info_country), "—")
                InfoRow(stringResource(R.string.vpn_info_ip), "—")
                InfoRow(stringResource(R.string.vpn_info_protocol), "WireGuard")
            }
        }

        Text(
            text = stringResource(R.string.vpn_preview_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

/**
 * Büyük yuvarlak bağlan düğmesi.
 * Kapalıyken daire, bağlanırken dönen yuvarlatılmış kare, bağlıyken "nefes alan" kare.
 * (Expressive hissi için şekil dönüşümü yay animasyonuyla yapılır.)
 */
@Composable
private fun ConnectButton(state: VpnState, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme

    val cornerPercent by animateIntAsState(
        targetValue = when (state) {
            VpnState.Off -> 50
            VpnState.Connecting -> 30
            VpnState.On -> 40
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "corner",
    )
    val shape = RoundedCornerShape(cornerPercent.coerceIn(0, 100))

    val container by animateColorAsState(
        targetValue = if (state == VpnState.On) cs.primary else cs.surfaceContainerHighest,
        animationSpec = tween(450),
        label = "container",
    )
    val onContainer by animateColorAsState(
        targetValue = if (state == VpnState.On) cs.onPrimary else cs.onSurface,
        animationSpec = tween(450),
        label = "on_container",
    )
    val haloAlpha = animateFloatAsState(
        targetValue = if (state == VpnState.On) 0.18f else 0f,
        animationSpec = tween(600),
        label = "halo",
    )

    val inf = rememberInfiniteTransition(label = "inf")
    val spin = inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing)),
        label = "spin",
    )
    val breathe = inf.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(1900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse,
        ),
        label = "breathe",
    )

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press = animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "press",
    )

    Box(
        modifier = Modifier
            .size(250.dp)
            .graphicsLayer {
                val b = if (state == VpnState.On) breathe.value else 1f
                val s = press.value * b
                scaleX = s
                scaleY = s
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Dış hale
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = haloAlpha.value }
                .clip(shape)
                .background(cs.primary)
        )
        // Ana gövde (bağlanırken döner)
        Box(
            Modifier
                .size(196.dp)
                .graphicsLayer {
                    rotationZ = if (state == VpnState.Connecting) spin.value else 0f
                }
                .clip(shape)
                .background(container)
        )
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "button_text",
        ) { s ->
            Text(
                text = stringResource(
                    when (s) {
                        VpnState.Off -> R.string.vpn_connect
                        VpnState.Connecting -> R.string.vpn_connecting
                        VpnState.On -> R.string.vpn_connected
                    }
                ),
                color = onContainer,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}
