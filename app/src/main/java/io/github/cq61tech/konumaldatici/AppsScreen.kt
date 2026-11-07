package io.github.cq61tech.konumaldatici

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.text.Collator
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppItem(
    val pkg: String,
    val label: String,
    val icon: ImageBitmap,
    val featured: Boolean,
)

/** Sekme değişince kaybolmasın diye ekranın dışında tutulan durum. */
class AppsState(private val settings: AppSettings) {
    var apps by mutableStateOf<List<AppItem>?>(null)
    var query by mutableStateOf("")

    var saved by mutableStateOf(settings.loadSelectedApps())
        private set
    var savedCountries by mutableStateOf(settings.loadAppCountries())
        private set

    val selected = mutableStateListOf<String>().also { it.addAll(saved) }

    /** Uygulama başına ülke (yoksa varsayılan ülke kullanılır) */
    val appCountry = mutableStateMapOf<String, String>().also { it.putAll(savedCountries) }

    private fun effectiveCountries(): Map<String, String> =
        appCountry.filterKeys { it in selected }

    val dirty: Boolean
        get() = selected.toSet() != saved || effectiveCountries() != savedCountries

    fun toggle(pkg: String) {
        if (!selected.remove(pkg)) selected.add(pkg)
    }

    fun save() {
        saved = selected.toSet()
        savedCountries = effectiveCountries()
        settings.saveAppSelection(saved, savedCountries)
    }
}

private val FEATURED = setOf(
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill",
    "com.instagram.android",
    "org.telegram.messenger",
    "org.telegram.messenger.web",
    "com.google.android.youtube",
    "com.whatsapp",
    "com.whatsapp.w4b",
    "com.twitter.android",
    "com.snapchat.android",
    "com.discord",
    "com.reddit.frontpage",
    "com.facebook.katana",
)

/** Kök erişimini / LSPosed'ı algılayıp açılışta takılan uygulamalar: seçmeden önce uyarılır. */
private val ROOT_DETECTING = setOf(
    "com.zhiliaoapp.musically",
    "com.ss.android.ugc.trill",
    "com.zhiliaoapp.musically.go",
)

/** Play Store ve Google Play hizmetleri listeden çıkarılır. */
private fun isProtected(pkg: String): Boolean =
    pkg == "com.android.vending" ||
        pkg == "com.google.android.gsf" ||
        pkg.startsWith("com.google.android.gms")

private fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val collator = Collator.getInstance(Locale.forLanguageTag("tr-TR"))
    return pm.getInstalledApplications(0)
        .asSequence()
        .filter { it.packageName != ctx.packageName }
        .filter { !isProtected(it.packageName) }
        .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
        .map { info ->
            AppItem(
                pkg = info.packageName,
                label = info.loadLabel(pm).toString(),
                icon = info.loadIcon(pm).toBitmap(96, 96).asImageBitmap(),
                featured = info.packageName in FEATURED,
            )
        }
        .sortedWith { a, b -> collator.compare(a.label, b.label) }
        .toList()
}

@Composable
fun AppsScreen(state: AppsState, settings: AppSettings, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val dockPad = LocalDockPadding.current
    var sheetPkg by remember { mutableStateOf<String?>(null) }
    var warnPkg by remember { mutableStateOf<String?>(null) }
    val defaultCountry = Countries.byCode(settings.defaultCountry)

    LaunchedEffect(Unit) {
        if (state.apps == null) {
            state.apps = withContext(Dispatchers.IO) { loadApps(ctx) }
        }
    }

    val apps = state.apps
    val q = state.query.trim()
    val filtered = remember(apps, q) {
        apps?.filter {
            q.isEmpty() ||
                it.label.contains(q, ignoreCase = true) ||
                it.pkg.contains(q, ignoreCase = true)
        }
    }

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TextField(
                value = state.query,
                onValueChange = { state.query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.apps_search)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Text(
                text = stringResource(R.string.apps_selected, state.selected.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                text = stringResource(R.string.apps_hidden_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp),
            )
            Text(
                text = stringResource(R.string.apps_country_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 2.dp),
            )

            if (filtered == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val featured = filtered.filter { it.featured }
                val rest = filtered.filter { !it.featured }
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = dockPad + 72.dp,
                    ),
                ) {
                    if (filtered.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = stringResource(R.string.apps_empty),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                            )
                        }
                    }
                    if (featured.isNotEmpty()) {
                        item(key = "h_featured") { SectionHeader(stringResource(R.string.apps_featured)) }
                        items(featured, key = { it.pkg }) { app ->
                            AppRow(
                                app = app,
                                selected = app.pkg in state.selected,
                                country = Countries.byCode(state.appCountry[app.pkg] ?: settings.defaultCountry),
                                overridden = app.pkg in state.appCountry,
                                onToggle = {
                                    if (app.pkg in ROOT_DETECTING && app.pkg !in state.selected) {
                                        warnPkg = app.pkg
                                    } else {
                                        state.toggle(app.pkg)
                                    }
                                },
                                onCountryClick = { sheetPkg = app.pkg },
                            )
                        }
                    }
                    if (rest.isNotEmpty()) {
                        item(key = "h_all") { SectionHeader(stringResource(R.string.apps_all)) }
                        items(rest, key = { it.pkg }) { app ->
                            AppRow(
                                app = app,
                                selected = app.pkg in state.selected,
                                country = Countries.byCode(state.appCountry[app.pkg] ?: settings.defaultCountry),
                                overridden = app.pkg in state.appCountry,
                                onToggle = {
                                    if (app.pkg in ROOT_DETECTING && app.pkg !in state.selected) {
                                        warnPkg = app.pkg
                                    } else {
                                        state.toggle(app.pkg)
                                    }
                                },
                                onCountryClick = { sheetPkg = app.pkg },
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = state.dirty,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = dockPad + 8.dp),
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    state.save()
                    Toast.makeText(ctx, R.string.apps_saved, Toast.LENGTH_SHORT).show()
                    ModuleBridge.requestScope(ctx, state.selected.toList())
                },
                icon = { Icon(Icons.Filled.Check, contentDescription = null) },
                text = { Text(stringResource(R.string.apps_save, state.selected.size)) },
            )
        }
    }

    warnPkg?.let { pkg ->
        AlertDialog(
            onDismissRequest = { warnPkg = null },
            title = { Text(stringResource(R.string.root_warn_title)) },
            text = { Text(stringResource(R.string.root_warn_text)) },
            confirmButton = {
                TextButton(onClick = {
                    state.toggle(pkg)
                    warnPkg = null
                }) { Text(stringResource(R.string.root_warn_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { warnPkg = null }) {
                    Text(stringResource(R.string.root_warn_cancel))
                }
            },
        )
    }

    sheetPkg?.let { pkg ->
        CountrySheet(
            selectedCode = state.appCountry[pkg],
            defaultCountry = defaultCountry,
            onPick = { code ->
                if (code == null) state.appCountry.remove(pkg) else state.appCountry[pkg] = code
                // Ülkesi seçilen uygulama otomatik seçili olsun
                if (code != null && pkg !in state.selected && pkg !in ROOT_DETECTING) state.selected.add(pkg)
                sheetPkg = null
            },
            onDismiss = { sheetPkg = null },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 6.dp),
    )
}

@Composable
private fun AppRow(
    app: AppItem,
    selected: Boolean,
    country: Country,
    overridden: Boolean,
    onToggle: () -> Unit,
    onCountryClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(
        targetValue = if (selected) cs.primaryContainer else cs.primaryContainer.copy(alpha = 0f),
        label = "row_bg",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            bitmap = app.icon,
            contentDescription = null,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        Column(Modifier.weight(1f)) {
            Text(
                app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                app.pkg,
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (overridden) cs.primary else cs.surfaceContainerHighest)
                    .clickable(onClick = onCountryClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${country.flag} ${country.code}",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (overridden) cs.onPrimary else cs.onSurface,
                )
            }
        }
        Checkbox(checked = selected, onCheckedChange = null)
    }
}
