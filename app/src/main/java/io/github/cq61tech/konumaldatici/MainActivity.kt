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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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

            AppTheme(settings.themeMode, settings.dynamicColor, settings.seedHue) {
                AppRoot(settings)
            }
        }
    }
}

private val dockItems = listOf(
    DockItem(R.string.nav_home, Icons.Filled.Home),
    DockItem(R.string.nav_vpn, Icons.Filled.Lock),
    DockItem(R.string.nav_apps, Icons.Filled.Menu),
    DockItem(R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
private fun AppRoot(settings: AppSettings) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val appsState = remember { AppsState(settings) }
    val cs = MaterialTheme.colorScheme

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Dock yüksekliği 72 + alt kenar boşluğu 12 + içerikle arasında 16
    val dockPadding = navBottom + 100.dp

    CompositionLocalProvider(LocalDockPadding provides dockPadding) {
        Box(
            Modifier
                .fillMaxSize()
                .background(cs.background)
        ) {
            AnimatedContent(
                targetState = tab,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                transitionSpec = {
                    (fadeIn(tween(240, delayMillis = 70)) +
                        scaleIn(tween(240, delayMillis = 70), initialScale = 0.96f)) togetherWith
                        fadeOut(tween(90))
                },
                label = "tab",
            ) { t ->
                when (t) {
                    0 -> HomeScreen()
                    1 -> VpnScreen(settings)
                    2 -> AppsScreen(appsState, settings)
                    else -> SettingsScreen(settings)
                }
            }

            // Dock'un arkasında içeriğin yumuşakça solması için
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(dockPadding + 12.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.7f to cs.background.copy(alpha = 0.9f),
                            1f to cs.background,
                        )
                    )
            )

            FloatingDock(
                items = dockItems,
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = navBottom + 12.dp),
            )
        }
    }
}
