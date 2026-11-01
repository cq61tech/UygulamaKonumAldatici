package io.github.cq61tech.konumaldatici

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

/** Uygulama ayarları. Aşama 3'te modül tarafıyla paylaşım (remote prefs) buraya bağlanacak. */
class AppSettings(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var themeMode by mutableStateOf(
        runCatching { ThemeMode.valueOf(sp.getString("theme", null) ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
    )
        private set

    var dynamicColor by mutableStateOf(sp.getBoolean("dynamic", true))
        private set

    var hookLog by mutableStateOf(sp.getBoolean("hook_log", true))
        private set

    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        sp.edit().putString("theme", mode.name).apply()
    }

    fun updateDynamic(value: Boolean) {
        dynamicColor = value
        sp.edit().putBoolean("dynamic", value).apply()
    }

    fun updateHookLog(value: Boolean) {
        hookLog = value
        sp.edit().putBoolean("hook_log", value).apply()
    }

    fun loadSelectedApps(): Set<String> =
        sp.getStringSet("selected_apps", emptySet())?.toSet() ?: emptySet()

    fun saveSelectedApps(apps: Set<String>) {
        sp.edit().putStringSet("selected_apps", HashSet(apps)).apply()
    }
}
