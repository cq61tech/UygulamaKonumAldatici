package io.github.cq61tech.konumaldatici.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private const val STORE_PACKAGE = "com.cq61tech.cqstore"
private const val STORE_URL = "https://github.com/cq61tech/cq-store/releases/latest"
private const val PREFS_NAME = "cq_store_prompt"
private const val KEY_DISMISSED = "dismissed"

private var checkedInSession = false

@Composable
fun CqStorePromptDialog() {
    val context = LocalContext.current
    var visible by rememberSaveable {
        val show = !checkedInSession && shouldShowPrompt(context)
        if (show) checkedInSession = true
        mutableStateOf(show)
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = { visible = false },
        title = { Text("We strongly recommend using CQ Store") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Get all my modules, updates, and changelogs in one place instead of checking GitHub releases manually.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Don't miss any updates or new apps!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    visible = false
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(STORE_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            ) {
                Text("Get CQ Store")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean(KEY_DISMISSED, true)
                            .apply()
                        visible = false
                    }
                ) {
                    Text("Don't show again")
                }
                TextButton(onClick = { visible = false }) {
                    Text("Later")
                }
            }
        },
    )
}

@Suppress("DEPRECATION")
private fun shouldShowPrompt(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (prefs.getBoolean(KEY_DISMISSED, false)) return false
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                STORE_PACKAGE,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            context.packageManager.getPackageInfo(STORE_PACKAGE, 0)
        }
        false
    } catch (_: PackageManager.NameNotFoundException) {
        true
    }
}
