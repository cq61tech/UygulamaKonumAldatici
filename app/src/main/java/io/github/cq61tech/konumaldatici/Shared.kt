package io.github.cq61tech.konumaldatici

/** Uygulama ile hook'lar arasında paylaşılan uzak ayar anahtarları. */
object Keys {
    const val GROUP = "konum"

    const val APPS = "apps"
    const val DEFAULT_COUNTRY = "default_country"
    const val LAYER_LOCATION = "layer_location"
    const val LAYER_SIM = "layer_sim"
    const val LAYER_TZ = "layer_tz"
    const val LAYER_LOCALE = "layer_locale"
    const val HOOK_LOG = "hook_log"

    fun countryKey(pkg: String) = "country.$pkg"
}
