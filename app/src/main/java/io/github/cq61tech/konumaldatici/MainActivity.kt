package io.github.cq61tech.konumaldatici

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activity = this
        val settings = AppSettings(this)

        setContent {
            val dark = isDarkFor(settings.themeMode)

            // Uygulama teması sistemden farklıysa durum/gezinme çubuğu simgeleri de uyum sağlasın
            DisposableEffect(dark) {
                activity.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT,
                    ) { dark },
                    navigationBarStyle = SystemBarStyle.auto(
                        AndroidColor.TRANSPARENT,
                        AndroidColor.TRANSPARENT,
                    ) { dark },
                )
                onDispose { }
            }

            AppTheme(settings.themeMode, settings.dynamicColor) {
                AppRoot(settings)
            }
        }
    }
}

private data class NavItem(val label: Int, val icon: ImageVector)

private val navItems = listOf(
    NavItem(R.string.nav_home, Icons.Filled.Home),
    NavItem(R.string.nav_vpn, Icons.Filled.Lock),
    NavItem(R.string.nav_apps, Icons.Filled.Menu),
    NavItem(R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
private fun AppRoot(settings: AppSettings) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val appsState = remember { AppsState(settings) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.label)) },
                    )
                }
            }
        },
    ) { inner ->
        AnimatedContent(
            targetState = tab,
            modifier = Modifier
                .padding(inner)
                .fillMaxSize(),
            transitionSpec = {
                (fadeIn(tween(240, delayMillis = 70)) +
                    scaleIn(tween(240, delayMillis = 70), initialScale = 0.96f)) togetherWith
                    fadeOut(tween(90))
            },
            label = "tab",
        ) { t ->
            when (t) {
                0 -> HomeScreen()
                1 -> VpnScreen()
                2 -> AppsScreen(appsState)
                else -> SettingsScreen(settings)
            }
        }
    }
}
