package io.github.cq61tech.konumaldatici.hook

import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.NetworkCapabilities
import android.os.Process
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "KonumAldatici"

/**
 * Hedef uygulamanın içinde çalışan hook motoru (libxposed API 101/102).
 *
 * Güvenlik önlemleri:
 * - Sadece seçili uygulamalarda ve sadece açık katmanlar için hook kurulur (kapalı katman = hiç hook yok).
 * - Hook'lar kendi içinde tekrar tetiklenirse (özyineleme) orijinal metoda düşer.
 * - Üretilen TimeZone/ZoneId/Locale nesneleri önbelleğe alınır, her çağrıda yenisi yaratılmaz.
 * - Günlük yazma ayrı bir iş parçacığında yapılır, uygulamanın ana akışını bekletmez.
 */
class HookEntry : XposedModule() {

    private var prefs: SharedPreferences? = null
    private val installed = AtomicBoolean(false)
    private val logged = HashSet<String>()
    private val busy = ThreadLocal<Boolean>()

    private val logExec: ExecutorService by lazy {
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "KonumAldatici-log").apply { isDaemon = true }
        }
    }

    private val tzCache = ConcurrentHashMap<String, TimeZone>()
    private val zoneCache = ConcurrentHashMap<String, ZoneId>()
    private val localeCache = ConcurrentHashMap<String, Locale>()

    private class Snap(
        val enabled: Boolean,
        val country: Country,
        val location: Boolean,
        val sim: Boolean,
        val tz: Boolean,
        val locale: Boolean,
        val vpnHide: Boolean,
        val log: Boolean,
    )

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
            val off = Snap(false, Countries.byCode(null), false, false, false, false, false, false)
            if (p == null) return off
            return try {
                val code = p.getString(Keys.countryKey(pkg), null)
                    ?: p.getString(Keys.DEFAULT_COUNTRY, "US")
                Snap(
                    enabled = p.getStringSet(Keys.APPS, null)?.contains(pkg) == true,
                    country = Countries.byCode(code),
                    location = p.getBoolean(Keys.LAYER_LOCATION, true),
                    sim = p.getBoolean(Keys.LAYER_SIM, true),
                    tz = p.getBoolean(Keys.LAYER_TZ, false),
                    locale = p.getBoolean(Keys.LAYER_LOCALE, false),
                    vpnHide = p.getBoolean(Keys.LAYER_VPNHIDE, true),
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
        if (!installed.compareAndSet(false, true)) return

        val pkg = param.packageName
        val cfg = Cfg(pkg)
        val s = cfg.get()
        log(
            Log.INFO, TAG,
            "yüklendi: $pkg pid=${Process.myPid()} (uzak tercihler: ${if (prefs != null) "var" else "yok"}; " +
                "seçili: ${s.enabled}; konum=${s.location} sim=${s.sim} saat=${s.tz} dil=${s.locale} vpnGizle=${s.vpnHide})"
        )
        if (!s.enabled) return

        if (s.location) safe("location") { hookLocation(cfg, pkg) }
        if (s.sim) safe("telephony") { hookTelephony(cfg) }
        if (s.tz) safe("timezone") { hookTimezone(cfg) }
        if (s.locale) safe("locale") { hookLocale(cfg) }
        if (s.vpnHide) safe("vpnhide") { hookVpnHide(cfg) }
        log(Log.INFO, TAG, "hook'lar kuruldu: $pkg")
    }

    private inline fun safe(name: String, block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            log(Log.WARN, TAG, "hook kurulamadı: $name", t)
        }
    }

    private fun note(s: Snap, key: String) {
        if (!s.log) return
        val first = synchronized(logged) { logged.add(key) }
        if (!first) return
        val msg = "spoof: $key -> ${s.country.code}"
        logExec.execute {
            busy.set(true)
            try {
                log(Log.INFO, TAG, msg)
            } catch (t: Throwable) {
            }
        }
    }

    private inline fun spoof(
        cfg: Cfg,
        layer: (Snap) -> Boolean,
        proceed: () -> Any?,
        value: (Snap) -> Any?,
    ): Any? {
        if (busy.get() == true) return proceed()
        var result: Any? = null
        var done = false
        busy.set(true)
        try {
            val s = cfg.get()
            if (s.enabled && layer(s)) {
                result = value(s)
                done = true
            }
        } catch (t: Throwable) {
            done = false
        } finally {
            busy.set(false)
        }
        return if (done) result else proceed()
    }

    private fun hookLocation(cfg: Cfg, pkg: String) {
        val h = pkg.hashCode()
        val dLat = (((h and 0xFF) - 128) / 128.0) * 0.0015
        val dLon = ((((h shr 8) and 0xFF) - 128) / 128.0) * 0.0015

        val getLat = Location::class.java.getDeclaredMethod("getLatitude")
        hook(getLat).intercept { chain ->
            spoof(cfg, { it.location }, { chain.proceed() }) { s ->
                note(s, "Location.getLatitude")
                s.country.lat + dLat
            }
        }

        val getLon = Location::class.java.getDeclaredMethod("getLongitude")
        hook(getLon).intercept { chain ->
            spoof(cfg, { it.location }, { chain.proceed() }) { s ->
                note(s, "Location.getLongitude")
                s.country.lon + dLon
            }
        }

        val fromMock = Location::class.java.getDeclaredMethod("isFromMockProvider")
        hook(fromMock).intercept { chain ->
            spoof(cfg, { it.location }, { chain.proceed() }) { false }
        }
        runCatching { Location::class.java.getDeclaredMethod("isMock") }.getOrNull()?.let { m ->
            hook(m).intercept { chain ->
                spoof(cfg, { it.location }, { chain.proceed() }) { false }
            }
        }

        val lastKnown = LocationManager::class.java.getDeclaredMethod(
            "getLastKnownLocation",
            String::class.java,
        )
        hook(lastKnown).intercept { chain ->
            val real = chain.proceed()
            if (real != null) {
                real
            } else {
                val provider = (chain.getArg(0) as? String) ?: LocationManager.GPS_PROVIDER
                spoof(cfg, { it.location }, { null }) { s ->
                    note(s, "LocationManager.getLastKnownLocation")
                    Location(provider).apply {
                        latitude = s.country.lat + dLat
                        longitude = s.country.lon + dLon
                        accuracy = 8f
                        time = System.currentTimeMillis()
                        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                    }
                }
            }
        }
    }

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
                    spoof(cfg, { it.sim }, { chain.proceed() }) { s ->
                        note(s, "TelephonyManager.${m.name}")
                        val c = s.country
                        when (kind) {
                            0 -> c.code.lowercase(Locale.ROOT)
                            1 -> c.mcc + c.mnc
                            else -> c.operatorName
                        }
                    }
                }
            } catch (t: Throwable) {
                log(Log.WARN, TAG, "hook kurulamadı: TelephonyManager.${m.name}", t)
            }
        }

        val subIso = SubscriptionInfo::class.java.getDeclaredMethod("getCountryIso")
        hook(subIso).intercept { chain ->
            spoof(cfg, { it.sim }, { chain.proceed() }) { s ->
                s.country.code.lowercase(Locale.ROOT)
            }
        }
    }

    private fun hookTimezone(cfg: Cfg) {
        val getDefault = TimeZone::class.java.getDeclaredMethod("getDefault")
        hook(getDefault).intercept { chain ->
            spoof(cfg, { it.tz }, { chain.proceed() }) { s ->
                note(s, "TimeZone.getDefault")
                val id = s.country.timezone
                val base = tzCache.getOrPut(id) { TimeZone.getTimeZone(id) }
                base.clone() as TimeZone
            }
        }

        val zoneDefault = ZoneId::class.java.getDeclaredMethod("systemDefault")
        hook(zoneDefault).intercept { chain ->
            spoof(cfg, { it.tz }, { chain.proceed() }) { s ->
                val id = s.country.timezone
                zoneCache.getOrPut(id) { ZoneId.of(id) }
            }
        }
    }

    private fun hookVpnHide(cfg: Cfg) {
        val intType = Int::class.javaPrimitiveType

        val hasTransport = NetworkCapabilities::class.java.getDeclaredMethod("hasTransport", intType)
        hook(hasTransport).intercept { chain ->
            if ((chain.getArg(0) as? Int) == NetworkCapabilities.TRANSPORT_VPN) {
                spoof(cfg, { it.vpnHide }, { chain.proceed() }) { s ->
                    note(s, "NetworkCapabilities.hasTransport(VPN)")
                    false
                }
            } else {
                chain.proceed()
            }
        }

        val hasCapability = NetworkCapabilities::class.java.getDeclaredMethod("hasCapability", intType)
        hook(hasCapability).intercept { chain ->
            if ((chain.getArg(0) as? Int) == NetworkCapabilities.NET_CAPABILITY_NOT_VPN) {
                spoof(cfg, { it.vpnHide }, { chain.proceed() }) { true }
            } else {
                chain.proceed()
            }
        }
    }

    private fun hookLocale(cfg: Cfg) {
        val plain = Locale::class.java.getDeclaredMethod("getDefault")
        hook(plain).intercept { chain ->
            spoof(cfg, { it.locale }, { chain.proceed() }) { s ->
                note(s, "Locale.getDefault")
                val tag = s.country.locale.replace('_', '-')
                localeCache.getOrPut(tag) { Locale.forLanguageTag(tag) }
            }
        }
        val withCategory = Locale::class.java.getDeclaredMethod("getDefault", Locale.Category::class.java)
        hook(withCategory).intercept { chain ->
            spoof(cfg, { it.locale }, { chain.proceed() }) { s ->
                val tag = s.country.locale.replace('_', '-')
                localeCache.getOrPut(tag) { Locale.forLanguageTag(tag) }
            }
        }
    }
}
