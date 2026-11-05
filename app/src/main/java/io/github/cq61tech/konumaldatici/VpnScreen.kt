package io.github.cq61tech.konumaldatici

import android.app.Activity
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.floor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class VpnState { Off, Connecting, On }

@Composable
fun VpnScreen(settings: AppSettings, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val state = VpnController.state
    val scope = rememberCoroutineScope()
    val lang = LocalConfiguration.current.locales[0].language
    val country = Countries.byCode(settings.defaultCountry)
    var showCountrySheet by remember { mutableStateOf(false) }
    var profiles by remember { mutableStateOf(WgStore.list(ctx)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { VpnController.syncRunning(ctx) }
    }

    // Kullanılacak profil: seçili olan, yoksa hedef ülkeyle eşleşen, yoksa ilk profil
    val profile = profiles.firstOrNull { it.id == settings.vpnProfile }
        ?: profiles.firstOrNull { it.country == settings.defaultCountry }
        ?: profiles.firstOrNull()

    fun startVpn() {
        val p = profile ?: return
        VpnController.connect(ctx, p, settings.loadSelectedApps() + ctx.packageName)
    }

    val prepareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { r ->
        if (r.resultCode == Activity.RESULT_OK) startVpn()
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        var ok = 0
        uris.forEach { if (WgStore.import(ctx, it) != null) ok++ }
        profiles = WgStore.list(ctx)
        Toast.makeText(
            ctx,
            ctx.getString(R.string.vpn_imported, ok, uris.size),
            Toast.LENGTH_SHORT,
        ).show()
    }

    fun toggle() {
        when (state) {
            VpnState.Connecting -> Unit
            VpnState.On -> VpnController.disconnect(ctx)
            VpnState.Off -> {
                if (profile == null) {
                    Toast.makeText(ctx, R.string.vpn_no_profile, Toast.LENGTH_LONG).show()
                    return
                }
                if (settings.loadSelectedApps().isEmpty()) {
                    Toast.makeText(ctx, R.string.vpn_no_apps, Toast.LENGTH_LONG).show()
                    return
                }
                val intent = VpnService.prepare(ctx)
                if (intent != null) prepareLauncher.launch(intent) else startVpn()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = LocalDockPadding.current),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(4.dp))

        Appear(0) { ConnectButton(state) { toggle() } }

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

        VpnController.error?.let { err ->
            Text(
                text = stringResource(R.string.vpn_error, err),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }

        Appear(1) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .clickable { showCountrySheet = true },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(country.flag, fontSize = 40.sp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.vpn_country_title),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            country.displayName(lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${country.operatorName} · ${country.timezone}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            stringResource(R.string.vpn_country_desc),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        Appear(2) {
            SectionCard(stringResource(R.string.vpn_profiles_title)) {
                if (profiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.vpn_profiles_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        profiles.forEach { p ->
                            FilterChip(
                                selected = p.id == profile?.id,
                                enabled = state == VpnState.Off,
                                onClick = {
                                    settings.updateVpnProfile(p.id)
                                    if (Countries.all.any { c -> c.code == p.country }) {
                                        settings.updateDefaultCountry(p.country)
                                    }
                                },
                                label = { Text("${flagOf(p.country)} ${p.id}") },
                            )
                        }
                    }
                }
                TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) {
                    Text(stringResource(R.string.vpn_import))
                }
            }
        }

        Appear(3) {
            SectionCard(stringResource(R.string.nav_vpn)) {
                InfoRow(
                    stringResource(R.string.vpn_info_country),
                    "${country.flag} ${country.displayName(lang)}",
                )
                InfoRow(stringResource(R.string.vpn_info_ip), IpCheck.text ?: "—")
                InfoRow(stringResource(R.string.vpn_info_protocol), "WireGuard")
                InfoRow(
                    stringResource(R.string.vpn_info_apps),
                    settings.loadSelectedApps().size.toString(),
                )
                TextButton(
                    enabled = !IpCheck.busy,
                    onClick = { scope.launch { IpCheck.run() } },
                ) {
                    Text(stringResource(R.string.vpn_check_ip))
                }
            }
        }

        Text(
            text = stringResource(R.string.vpn_scope_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }

    if (showCountrySheet) {
        CountrySheet(
            selectedCode = settings.defaultCountry,
            defaultCountry = null,
            onPick = { code ->
                if (code != null) {
                    settings.updateDefaultCountry(code)
                    profiles.firstOrNull { it.country == code }?.let { settings.updateVpnProfile(it.id) }
                }
                showCountrySheet = false
            },
            onDismiss = { showCountrySheet = false },
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
 * Büyük bağlan düğmesi.
 * Kapalı: daire. Bağlanıyor: Soft Burst → Cookie 9 → Pentagon → Pill → Sunny → Cookie 4 → Oval
 * şekilleri arasında dönüşerek döner. Bağlı: nefes alan kurabiye şekli.
 */
@Composable
private fun ConnectButton(state: VpnState, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme

    val soft = spring<Float>(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow)
    val onW = animateFloatAsState(
        targetValue = if (state == VpnState.On) 1f else 0f,
        animationSpec = soft,
        label = "on_weight",
    )
    val connW = animateFloatAsState(
        targetValue = if (state == VpnState.Connecting) 1f else 0f,
        animationSpec = soft,
        label = "connecting_weight",
    )
    val haloAlpha = animateFloatAsState(
        targetValue = if (state == VpnState.On) 0.18f else 0f,
        animationSpec = tween(600),
        label = "halo",
    )
    val container = animateColorAsState(
        targetValue = if (state == VpnState.On) cs.primary else cs.surfaceContainerHighest,
        animationSpec = tween(450),
        label = "container",
    )
    val onContainer by animateColorAsState(
        targetValue = if (state == VpnState.On) cs.onPrimary else cs.onSurface,
        animationSpec = tween(450),
        label = "on_container",
    )

    val inf = rememberInfiniteTransition(label = "inf")
    val cycle = inf.animateFloat(
        initialValue = 0f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(tween(7 * 850, easing = LinearEasing)),
        label = "cycle",
    )
    val spin = inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "spin",
    )
    val breathe = inf.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
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

    val base = remember { FloatArray(ShapeBank.N) }
    val cyc = remember { FloatArray(ShapeBank.N) }
    val out = remember { FloatArray(ShapeBank.N) }
    val path = remember { Path() }

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
        Canvas(Modifier.fillMaxSize()) {
            val half = size.minDimension / 2f
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Taban: daire ↔ kurabiye (bağlıyken)
            ShapeBank.mix(ShapeBank.circle, ShapeBank.cookie9, onW.value, base)

            // Bağlanırken şekil dizisi arasında dönüşüm
            val cw = connW.value
            var rotation = 0f
            if (cw > 0.001f) {
                val t = cycle.value
                val i0 = floor(t).toInt() % 7
                val frac = t - floor(t)
                val k = ((frac - 0.3f) / 0.7f).coerceIn(0f, 1f)
                val eased = k * k * (3f - 2f * k)
                ShapeBank.mix(
                    ShapeBank.sequence[i0],
                    ShapeBank.sequence[(i0 + 1) % 7],
                    eased,
                    cyc,
                )
                ShapeBank.mix(base, cyc, cw, out)
                rotation = spin.value * cw
            } else {
                ShapeBank.copy(base, out)
            }

            withTransform({ rotate(rotation, androidx.compose.ui.geometry.Offset(cx, cy)) }) {
                val ha = haloAlpha.value
                if (ha > 0.001f) {
                    drawPath(ShapeBank.path(out, cx, cy, half * 0.98f, path), cs.primary.copy(alpha = ha))
                }
                drawPath(ShapeBank.path(out, cx, cy, half * 0.78f, path), container.value)
            }
        }

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
