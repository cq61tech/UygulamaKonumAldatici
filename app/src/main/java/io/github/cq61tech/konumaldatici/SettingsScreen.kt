package io.github.cq61tech.konumaldatici

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import kotlin.math.abs
import kotlin.math.roundToInt

private const val GITHUB_URL = "https://github.com/cq61tech/UygulamaKonumAldatici"

/** Hazır ton örnekleri (renk çarkı üzerindeki derece) */
private val HUE_PRESETS = listOf(0f, 25f, 45f, 130f, 175f, 215f, 255f, 285f, 330f)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: AppSettings, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val version = remember { appVersion(ctx) }

    val currentTags = remember { AppCompatDelegate.getApplicationLocales().toLanguageTags() }
    var lang by remember {
        mutableStateOf(
            when {
                currentTags.startsWith("tr") -> "tr"
                currentTags.startsWith("en") -> "en"
                else -> ""
            }
        )
    }
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dynamicOn = settings.dynamicColor && dynamicSupported

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .padding(bottom = LocalDockPadding.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Appear(0) {
            SectionCard(stringResource(R.string.settings_theme)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.themeMode == mode,
                            onClick = { settings.updateTheme(mode) },
                            label = {
                                Text(
                                    stringResource(
                                        when (mode) {
                                            ThemeMode.SYSTEM -> R.string.theme_system
                                            ThemeMode.LIGHT -> R.string.theme_light
                                            ThemeMode.DARK -> R.string.theme_dark
                                            ThemeMode.AMOLED -> R.string.theme_amoled
                                        }
                                    )
                                )
                            },
                        )
                    }
                }
                SwitchRow(
                    title = stringResource(R.string.settings_dynamic),
                    desc = stringResource(
                        if (dynamicSupported) R.string.settings_dynamic_desc
                        else R.string.settings_dynamic_unsupported
                    ),
                    checked = dynamicOn,
                    enabled = dynamicSupported,
                    onChange = { settings.updateDynamic(it) },
                )

                // Monet kapalıyken kendi renk paletini seç
                AnimatedVisibility(visible = !dynamicOn) {
                    Column(Modifier.padding(top = 16.dp)) {
                        Text(
                            stringResource(R.string.settings_palette),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Swatch(
                                color = Color(0xFF4B5BD6),
                                selected = settings.seedHue < 0f,
                                onClick = { settings.updateSeedHue(-1f) },
                            )
                            HUE_PRESETS.forEach { h ->
                                Swatch(
                                    color = Color.hsl(h, 0.62f, 0.50f),
                                    selected = settings.seedHue >= 0f && abs(settings.seedHue - h) < 0.5f,
                                    onClick = { settings.updateSeedHue(h) },
                                )
                            }
                        }
                        Text(
                            stringResource(R.string.palette_hue),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        Slider(
                            value = if (settings.seedHue < 0f) 255f else settings.seedHue,
                            onValueChange = { settings.updateSeedHue(it) },
                            valueRange = 0f..360f,
                        )
                    }
                }
            }
        }

        Appear(1) {
            SectionCard(stringResource(R.string.settings_dock)) {
                SwitchRow(
                    title = stringResource(R.string.dock_pill_title),
                    desc = stringResource(R.string.dock_pill_desc),
                    checked = settings.dockPill,
                    enabled = true,
                    onChange = { settings.updateDockPill(it) },
                )

                // Alt ayar: dock seçenekleri
                var expanded by remember { mutableStateOf(false) }
                val arrow by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    label = "dock_options_arrow",
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { expanded = !expanded }
                        .padding(top = 16.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.dock_options),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.rotate(arrow),
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Column {
                        LabeledSlider(
                            label = stringResource(R.string.dock_height, settings.dockHeight),
                            value = settings.dockHeight.toFloat(),
                            range = 56f..88f,
                            steps = 7,
                            onChange = { settings.updateDockHeight(it.roundToInt()) },
                        )
                        LabeledSlider(
                            label = stringResource(
                                R.string.dock_transparency,
                                (settings.dockTransparency * 100f).roundToInt(),
                            ),
                            value = settings.dockTransparency * 100f,
                            range = 0f..60f,
                            steps = 0,
                            onChange = { settings.updateDockTransparency(it / 100f) },
                        )
                        LabeledSlider(
                            label = stringResource(
                                R.string.dock_roundness,
                                (settings.dockRoundness * 100f).roundToInt(),
                            ),
                            value = settings.dockRoundness * 100f,
                            range = 0f..100f,
                            steps = 0,
                            onChange = { settings.updateDockRoundness(it / 100f) },
                        )
                        SwitchRow(
                            title = stringResource(R.string.dock_autohide),
                            desc = stringResource(R.string.dock_autohide_desc),
                            checked = settings.dockAutoHide,
                            enabled = true,
                            onChange = { settings.updateDockAutoHide(it) },
                        )
                    }
                }
            }
        }

        Appear(2) {
            SectionCard(stringResource(R.string.settings_language)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        "" to R.string.lang_system,
                        "tr" to R.string.lang_tr,
                        "en" to R.string.lang_en,
                    ).forEach { (tag, label) ->
                        FilterChip(
                            selected = lang == tag,
                            onClick = {
                                lang = tag
                                AppCompatDelegate.setApplicationLocales(
                                    if (tag.isEmpty()) LocaleListCompat.getEmptyLocaleList()
                                    else LocaleListCompat.forLanguageTags(tag)
                                )
                            },
                            label = { Text(stringResource(label)) },
                        )
                    }
                }
            }
        }

        Appear(3) {
            SectionCard(stringResource(R.string.layers_title)) {
                SwitchRow(
                    title = stringResource(R.string.layer_location),
                    desc = stringResource(R.string.layer_location_desc),
                    checked = settings.layerLocation,
                    enabled = true,
                    onChange = { settings.updateLayer(Keys.LAYER_LOCATION, it) },
                )
                SwitchRow(
                    title = stringResource(R.string.layer_sim),
                    desc = stringResource(R.string.layer_sim_desc),
                    checked = settings.layerSim,
                    enabled = true,
                    onChange = { settings.updateLayer(Keys.LAYER_SIM, it) },
                )
                SwitchRow(
                    title = stringResource(R.string.layer_tz),
                    desc = stringResource(R.string.layer_tz_desc),
                    checked = settings.layerTimezone,
                    enabled = true,
                    onChange = { settings.updateLayer(Keys.LAYER_TZ, it) },
                )
                SwitchRow(
                    title = stringResource(R.string.layer_locale),
                    desc = stringResource(R.string.layer_locale_desc),
                    checked = settings.layerLocale,
                    enabled = true,
                    onChange = { settings.updateLayer(Keys.LAYER_LOCALE, it) },
                )
            }
        }

        Appear(4) {
            SectionCard(stringResource(R.string.settings_hooklog)) {
                SwitchRow(
                    title = stringResource(R.string.settings_hooklog),
                    desc = stringResource(R.string.settings_hooklog_desc),
                    checked = settings.hookLog,
                    enabled = true,
                    onChange = { settings.updateHookLog(it) },
                )
            }
        }

        Appear(5) {
            SectionCard(stringResource(R.string.settings_about)) {
                Text(
                    text = stringResource(R.string.app_name) + "  •  " +
                        stringResource(R.string.home_version, version.first, version.second),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = stringResource(R.string.about_license),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(
                    onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL)))
                    },
                ) {
                    Text(stringResource(R.string.about_github))
                }
            }
        }
    }
}

@Composable
private fun Swatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (selected) Modifier.border(3.dp, cs.onSurface, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
        )
    }
}
