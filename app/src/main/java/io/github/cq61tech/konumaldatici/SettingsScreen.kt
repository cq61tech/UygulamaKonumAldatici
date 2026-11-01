package io.github.cq61tech.konumaldatici

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat

private const val GITHUB_URL = "https://github.com/cq61tech/UygulamaKonumAldatici"

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                SwitchRow(
                    title = stringResource(R.string.settings_dynamic),
                    desc = stringResource(
                        if (dynamicSupported) R.string.settings_dynamic_desc
                        else R.string.settings_dynamic_unsupported
                    ),
                    checked = settings.dynamicColor && dynamicSupported,
                    enabled = dynamicSupported,
                    onChange = { settings.updateDynamic(it) },
                )
            }
        }

        Appear(1) {
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

        Appear(2) {
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

        Appear(3) {
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
