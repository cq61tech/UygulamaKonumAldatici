package io.github.cq61tech.konumaldatici.hook

import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import android.telephony.SubscriptionInfo
import android.telephony.TelephonyManager
import android.util.Log
import io.github.cq61tech.konumaldatici.Countries
import io.github.cq61tech.konumaldatici.Country
import io.github.cq61tech.konumaldatici.Keys
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import java.time.ZoneId
import java.util.Locale
import java.util.TimeZone

private const val TAG = "KonumAldatici"

/**
 * Hedef uygulamanın içinde çalışan hook motoru (libxposed API 101/102).
 * Hangi uygulamanın hangi ülkede görüneceği, uygulamanın yazdığı uzak tercihlerden okunur.
 * Her hook ayrı ayrı korunur: biri başarısız olursa diğerleri çalışmaya devam eder.
 */
class HookEntry : XposedModule() {

    private var prefs: SharedPreferences? = null
    private val logged = HashSet<String>()

    private class Snap(
        val enabled: Boolean,
        val country: Country,
        val location: Boolean,
        val sim: Boolean,
        val tz: Boolean,
        val locale: Boolean,
        val log: Boolean,
    )

    /** Uzak tercihleri her çağrıda değil, yarım saniyede bir okuyan küçük önbellek. */
    private inner class Cfg(private val pkg: String) {
        @Volatile
        private var snap: Snap? = null

        @Volatile
        private var stamp = 0L

        fun get(): Snap {
            val now = SystemClock.elapsedRealtime()
            val cached = snap
            if (cached != null && now - stamp < 500L) return cached
            val fresh = read()
            snap = fresh
            stamp = now
            return fresh
        }

        private fun read(): Snap {
            val p = prefs
            val off = Snap(false, Countries.byCode(null), false, false, false, false, false)
            if (p == null) return off
            return try {
                val code = p.getString(Keys.countryKey(pkg), null)
                    ?: p.getString(Keys.DEFAULT_COUNTRY, "US")
                Snap(
                    enabled = p.getStringSet(Keys.APPS, null)?.contains(pkg) == true,
                    country = Countries.byCode(code),
                    location = p.getBoolean(Keys.LAYER_LOCATION, true),
                    sim = p.getBoolean(Keys.LAYER_SIM, true),
                    tz = p.getBoolean(Keys.LAYER_TZ, true),
                    locale = p.getBoolean(Keys.LAYER_LOCALE, false),
                    log = p.getBoolean(Keys.HOOK_LOG, true),
                )
            } catch (t: Throwable) {
                off
            }
        }
    }

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        prefs = try {
            getRemotePreferences(Keys.GROUP)
        } catch (t: Throwable) {
            null
        }
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (!param.isFirstPackage) return
        val pkg = param.packageName
        val cfg = Cfg(pkg)
        log(Log.INFO, TAG, "yüklendi: $pkg (uzak tercihler: ${if (prefs != null) "var" else "yok"})")

        safe("location") { hookLocation(cfg, pkg) }
        safe("telephony") { hookTelephony(cfg) }
        safe("timezone") { hookTimezone(cfg) }
        safe("locale") { hookLocale(cfg) }
    }

    private inline fun safe(name: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            log(Log.WARN, TAG, "hook kurulamadı: $name", t)
        }
    }

    private fun note(cfg: Cfg, key: String) {
        val s = cfg.get()
        if (!s.log) return
        val first = synchronized(logged) { logged.add(key) }
        if (first) log(Log.INFO, TAG, "spoof: $key -> ${s.country.code}")
    }

    // ------------------------------------------------------------------ Konum

    private fun hookLocation(cfg: Cfg, pkg: String) {
        // Her uygulama için sabit, küçük bir sapma (yaklaşık 150 m): hep aynı noktada durmasın
        val h = pkg.hashCode()
        val dLat = (((h and 0xFF) - 128) / 128.0) * 0.0015
        val dLon = ((((h shr 8) and 0xFF) - 128) / 128.0) * 0.0015

        val getLat = Location::class.java.getDeclaredMethod("getLatitude")
        hook(getLat).intercept { chain ->
            val s = cfg.get()
            if (s.enabled && s.location) {
                note(cfg, "Location.getLatitude")
                s.country.lat + dLat
            } else {
                chain.proceed()
            }
        }
        deoptimize(getLat)

        val getLon = Location::class.java.getDeclaredMethod("getLongitude")
        hook(getLon).intercept { chain ->
            val s = cfg.get()
            if (s.enabled && s.location) {
                note(cfg, "Location.getLongitude")
                s.country.lon + dLon
            } else {
                chain.proceed()
            }
        }
        deoptimize(getLon)

        // Sahte konum sağlayıcı bayrağı: her zaman "gerçek" görünsün
        val fromMock = Location::class.java.getDeclaredMethod("isFromMockProvider")
        hook(fromMock).intercept { chain ->
            val s = cfg.get()
            if (s.enabled && s.location) false else chain.proceed()
        }
        runCatching { Location::class.java.getDeclaredMethod("isMock") }.getOrNull()?.let { m ->
            hook(m).intercept { chain ->
                val s = cfg.get()
                if (s.enabled && s.location) false else chain.proceed()
            }
        }

        // Son bilinen konum yoksa seçili ülkeden bir tane üret
        val lastKnown = LocationManager::class.java.getDeclaredMethod(
            "getLastKnownLocation",
            String::class.java,
        )
        hook(lastKnown).intercept { chain ->
            val real = chain.proceed()
            val s = cfg.get()
            if (s.enabled && s.location && real == null) {
                note(cfg, "LocationManager.getLastKnownLocation")
                val provider = (chain.getArg(0) as? String) ?: LocationManager.GPS_PROVIDER
                Location(provider).apply {
                    latitude = s.country.lat + dLat
                    longitude = s.country.lon + dLon
                    accuracy = 8f
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }
            } else {
                real
            }
        }
    }

    // ------------------------------------------------------- SIM / operatör

    private fun hookTelephony(cfg: Cfg) {
        for (m in TelephonyManager::class.java.declaredMethods) {
            if (m.returnType != String::class.java) continue
            val kind = when (m.name) {
                "getSimCountryIso", "getNetworkCountryIso" -> 0
                "getSimOperator", "getNetworkOperator" -> 1
                "getSimOperatorName", "getNetworkOperatorName" -> 2
                else -> continue
            }
            try {
                hook(m).intercept { chain ->
                    val s = cfg.get()
                    if (s.enabled && s.sim) {
                        note(cfg, "TelephonyManager.${m.name}")
                        val c = s.country
                        when (kind) {
                            0 -> c.code.lowercase(Locale.ROOT)
                            1 -> c.mcc + c.mnc
                            else -> c.operatorName
                        }
                    } else {
                        chain.proceed()
                    }
                }
            } catch (t: Throwable) {
                log(Log.WARN, TAG, "hook kurulamadı: TelephonyManager.${m.name}", t)
            }
        }

        val subIso = SubscriptionInfo::class.java.getDeclaredMethod("getCountryIso")
        hook(subIso).intercept { chain ->
            val s = cfg.get()
            if (s.enabled && s.sim) s.country.code.lowercase(Locale.ROOT) else chain.proceed()
        }
    }

    // ------------------------------------------------------------ Saat dilimi

    private fun hookTimezone(cfg: Cfg) {
        val getDefault = TimeZone::class.java.getDeclaredMethod("getDefault")
        hook(getDefault).intercept { chain ->
            val s = cfg.get()
            if (s.enabled && s.tz) {
                note(cfg, "TimeZone.getDefault")
                TimeZone.getTimeZone(s.country.timezone)
            } else {
                chain.proceed()
            }
        }
        deoptimize(getDefault)

        val zoneDefault = ZoneId::class.java.getDeclaredMethod("systemDefault")
        hook(zoneDefault).intercept { chain ->
            val s = cfg.get()
            if (s.enabled && s.tz) ZoneId.of(s.country.timezone) else chain.proceed()
        }
    }

    // ------------------------------------------------------------ Bölge / dil

    private fun hookLocale(cfg: Cfg) {
        val plain = Locale::class.java.getDeclaredMethod("getDefault")
        hook(plain).intercept { chain ->
            val s = cfg.get()
            if (s.enabled && s.locale) {
                note(cfg, "Locale.getDefault")
                Locale.forLanguageTag(s.country.locale.replace('_', '-'))
            } else {
                chain.proceed()
            }
        }
        val withCategory = Locale::class.java.getDeclaredMethod("getDefault", Locale.Category::class.java)
        hook(withCategory).intercept { chain ->
            val s = cfg.get()
            if (s.enabled && s.locale) {
                Locale.forLanguageTag(s.country.locale.replace('_', '-'))
            } else {
                chain.proceed()
            }
        }
    }
}
