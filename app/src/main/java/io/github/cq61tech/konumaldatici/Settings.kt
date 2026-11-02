package io.github.cq61tech.konumaldatici

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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

    /** -1 = varsayılan gökyüzü paleti, 0..360 = seçilen ton */
    var seedHue by mutableFloatStateOf(sp.getFloat("seed_hue", -1f))
        private set

    var hookLog by mutableStateOf(sp.getBoolean("hook_log", true))
        private set

    /** VPN ve konum profili için varsayılan ülke (ülke kodu) */
    var defaultCountry by mutableStateOf(sp.getString("country", "US") ?: "US")
        private set

    /** Dock: true = yüzen hap, false = ekranın altına oturan düz çubuk */
    var dockPill by mutableStateOf(sp.getBoolean("dock_pill", true))
        private set

    /** Dock yüksekliği (dp) */
    var dockHeight by mutableIntStateOf(sp.getInt("dock_height", 72))
        private set

    /** 0 = tam opak, 0.6 = en şeffaf */
    var dockTransparency by mutableFloatStateOf(sp.getFloat("dock_transparency", 0f))
        private set

    /** 0 = köşeli, 1 = tam yuvarlak */
    var dockRoundness by mutableFloatStateOf(sp.getFloat("dock_roundness", 1f))
        private set

    var dockAutoHide by mutableStateOf(sp.getBoolean("dock_autohide", false))
        private set

    fun updateDockPill(value: Boolean) {
        dockPill = value
        sp.edit().putBoolean("dock_pill", value).apply()
    }

    fun updateDockHeight(value: Int) {
        dockHeight = value
        sp.edit().putInt("dock_height", value).apply()
    }

    fun updateDockTransparency(value: Float) {
        dockTransparency = value
        sp.edit().putFloat("dock_transparency", value).apply()
    }

    fun updateDockRoundness(value: Float) {
        dockRoundness = value
        sp.edit().putFloat("dock_roundness", value).apply()
    }

    fun updateDockAutoHide(value: Boolean) {
        dockAutoHide = value
        sp.edit().putBoolean("dock_autohide", value).apply()
    }

    /** Hangi katmanlar spooflansın (hook'lar bunları uzak tercihlerden okur) */
    var layerLocation by mutableStateOf(sp.getBoolean("layer_location", true))
        private set

    var layerSim by mutableStateOf(sp.getBoolean("layer_sim", true))
        private set

    var layerTimezone by mutableStateOf(sp.getBoolean("layer_tz", false))
        private set

    var layerLocale by mutableStateOf(sp.getBoolean("layer_locale", false))
        private set

    private fun sync() {
        ModuleBridge.pushAll()
    }

    fun updateLayer(key: String, value: Boolean) {
        when (key) {
            Keys.LAYER_LOCATION -> layerLocation = value
            Keys.LAYER_SIM -> layerSim = value
            Keys.LAYER_TZ -> layerTimezone = value
            Keys.LAYER_LOCALE -> layerLocale = value
        }
        sp.edit().putBoolean(key, value).apply()
        sync()
    }

    fun saveAppSelection(apps: Set<String>, countries: Map<String, String>) {
        sp.edit()
            .putStringSet("selected_apps", HashSet(apps))
            .putStringSet("app_countries", HashSet(countries.map { "${it.key}|${it.value}" }))
            .apply()
        sync()
    }

    fun updateTheme(mode: ThemeMode) {
        themeMode = mode
        sp.edit().putString("theme", mode.name).apply()
    }

    fun updateDynamic(value: Boolean) {
        dynamicColor = value
        sp.edit().putBoolean("dynamic", value).apply()
    }

    fun updateSeedHue(value: Float) {
        seedHue = value
        sp.edit().putFloat("seed_hue", value).apply()
    }

    fun updateHookLog(value: Boolean) {
        hookLog = value
        sp.edit().putBoolean("hook_log", value).apply()
        sync()
    }

    fun updateDefaultCountry(code: String) {
        defaultCountry = code
        sp.edit().putString("country", code).apply()
        sync()
    }

    fun loadSelectedApps(): Set<String> =
        sp.getStringSet("selected_apps", emptySet())?.toSet() ?: emptySet()

    fun saveSelectedApps(apps: Set<String>) {
        sp.edit().putStringSet("selected_apps", HashSet(apps)).apply()
    }

    /** Uygulama başına ülke seçimi: "paket|ÜLKE" biçiminde saklanır. */
    fun loadAppCountries(): Map<String, String> =
        (sp.getStringSet("app_countries", emptySet()) ?: emptySet())
            .mapNotNull { entry ->
                val i = entry.lastIndexOf('|')
                if (i <= 0) null else entry.substring(0, i) to entry.substring(i + 1)
            }
            .toMap()

    fun saveAppCountries(map: Map<String, String>) {
        sp.edit().putStringSet("app_countries", HashSet(map.map { "${it.key}|${it.value}" })).apply()
    }
}
