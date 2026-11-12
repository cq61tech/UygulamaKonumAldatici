package io.github.cq61tech.konumaldatici

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val FallbackLight = lightColorScheme(
    primary = Color(0xFF4B5BD6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE0FF),
    onPrimaryContainer = Color(0xFF00105C),
    secondary = Color(0xFF5B5D72),
    secondaryContainer = Color(0xFFE0E1F9),
    onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF9C4A6E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E7),
    onTertiaryContainer = Color(0xFF3D0525),
    background = Color(0xFFFBF8FF),
    surface = Color(0xFFFBF8FF),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9),
)

private val FallbackDark = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF0B1A8C),
    primaryContainer = Color(0xFF2E3DB4),
    onPrimaryContainer = Color(0xFFDDE0FF),
    secondary = Color(0xFFC3C5DD),
    secondaryContainer = Color(0xFF434559),
    onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFFFB0CE),
    onTertiary = Color(0xFF5E1139),
    tertiaryContainer = Color(0xFF7C3256),
    onTertiaryContainer = Color(0xFFFFD8E7),
    background = Color(0xFF121318),
    surface = Color(0xFF121318),
    surfaceContainerLowest = Color(0xFF0D0E13),
    surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
)

private fun hsl(h: Float, s: Float, l: Float): Color =
    Color.hsl(((h % 360f) + 360f) % 360f, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))

private fun schemeFromHue(h: Float, dark: Boolean): ColorScheme {
    val t = h + 60f
    return if (!dark) {
        lightColorScheme(
            primary = hsl(h, 0.55f, 0.40f),
            onPrimary = Color.White,
            primaryContainer = hsl(h, 0.80f, 0.90f),
            onPrimaryContainer = hsl(h, 0.80f, 0.12f),
            secondary = hsl(h, 0.18f, 0.40f),
            onSecondary = Color.White,
            secondaryContainer = hsl(h, 0.30f, 0.90f),
            onSecondaryContainer = hsl(h, 0.30f, 0.12f),
            tertiary = hsl(t, 0.45f, 0.40f),
            onTertiary = Color.White,
            tertiaryContainer = hsl(t, 0.70f, 0.90f),
            onTertiaryContainer = hsl(t, 0.70f, 0.12f),
            background = hsl(h, 0.25f, 0.98f),
            onBackground = hsl(h, 0.15f, 0.10f),
            surface = hsl(h, 0.25f, 0.98f),
            onSurface = hsl(h, 0.15f, 0.10f),
            surfaceVariant = hsl(h, 0.20f, 0.90f),
            onSurfaceVariant = hsl(h, 0.10f, 0.30f),
            outline = hsl(h, 0.08f, 0.45f),
            outlineVariant = hsl(h, 0.15f, 0.80f),
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = hsl(h, 0.25f, 0.96f),
            surfaceContainer = hsl(h, 0.20f, 0.94f),
            surfaceContainerHigh = hsl(h, 0.18f, 0.92f),
            surfaceContainerHighest = hsl(h, 0.16f, 0.90f),
        )
    } else {
        darkColorScheme(
            primary = hsl(h, 0.75f, 0.80f),
            onPrimary = hsl(h, 0.80f, 0.20f),
            primaryContainer = hsl(h, 0.55f, 0.30f),
            onPrimaryContainer = hsl(h, 0.85f, 0.90f),
            secondary = hsl(h, 0.30f, 0.78f),
            onSecondary = hsl(h, 0.30f, 0.20f),
            secondaryContainer = hsl(h, 0.20f, 0.30f),
            onSecondaryContainer = hsl(h, 0.40f, 0.90f),
            tertiary = hsl(t, 0.65f, 0.80f),
            onTertiary = hsl(t, 0.70f, 0.20f),
            tertiaryContainer = hsl(t, 0.45f, 0.30f),
            onTertiaryContainer = hsl(t, 0.70f, 0.90f),
            background = hsl(h, 0.15f, 0.07f),
            onBackground = hsl(h, 0.10f, 0.90f),
            surface = hsl(h, 0.15f, 0.07f),
            onSurface = hsl(h, 0.10f, 0.90f),
            surfaceVariant = hsl(h, 0.12f, 0.25f),
            onSurfaceVariant = hsl(h, 0.10f, 0.78f),
            outline = hsl(h, 0.06f, 0.60f),
            outlineVariant = hsl(h, 0.10f, 0.30f),
            surfaceContainerLowest = hsl(h, 0.15f, 0.05f),
            surfaceContainerLow = hsl(h, 0.15f, 0.10f),
            surfaceContainer = hsl(h, 0.14f, 0.12f),
            surfaceContainerHigh = hsl(h, 0.13f, 0.16f),
            surfaceContainerHighest = hsl(h, 0.12f, 0.20f),
        )
    }
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

@Composable
fun isDarkFor(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    else -> true
}

@Composable
fun AppTheme(mode: ThemeMode, dynamic: Boolean, seedHue: Float, content: @Composable () -> Unit) {
    val dark = isDarkFor(mode)
    val ctx = LocalContext.current
    val useDynamic = dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    var scheme = when {
        useDynamic && dark -> dynamicDarkColorScheme(ctx)
        useDynamic -> dynamicLightColorScheme(ctx)
        seedHue >= 0f -> schemeFromHue(seedHue, dark)
        dark -> FallbackDark
        else -> FallbackLight
    }

    if (mode == ThemeMode.AMOLED) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0A),
            surfaceContainer = Color(0xFF111111),
            surfaceContainerHigh = Color(0xFF181818),
            surfaceContainerHighest = Color(0xFF212121),
        )
    }

    MaterialTheme(colorScheme = scheme, shapes = AppShapes, content = content)
}
