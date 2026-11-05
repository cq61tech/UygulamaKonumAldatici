package io.github.cq61tech.konumaldatici

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper

/**
 * Uygulama tarafı: LSPosed ile konuşur.
 * - Servis bağlanınca framework bilgisini okur (ana ekrandaki durum kartı).
 * - Ayarları "uzak tercihler" olarak yazar; hook'lar hedef uygulamada bunları okur.
 * - Seçilen uygulamaları modül kapsamına eklemek için kullanıcıdan onay ister.
 */
object ModuleBridge {

    class Info(
        val name: String,
        val version: String,
        val versionCode: Long,
        val api: Int,
        val scope: List<String>,
    )

    var info by mutableStateOf<Info?>(null)
        private set

    private var service: XposedService? = null
    private var settings: AppSettings? = null
    private var registered = false
    private val main = Handler(Looper.getMainLooper())

    fun register() {
        if (registered) return
        registered = true
        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
            override fun onServiceBind(service: XposedService) {
                this@ModuleBridge.service = service
                refreshInfo(service)
                pushAll()
            }

            override fun onServiceDied(service: XposedService) {
                if (this@ModuleBridge.service === service) {
                    this@ModuleBridge.service = null
                    info = null
                }
            }
        })
    }

    fun attach(s: AppSettings) {
        settings = s
        pushAll()
    }

    private fun refreshInfo(s: XposedService) {
        info = try {
            Info(
                name = s.frameworkName,
                version = s.frameworkVersion,
                versionCode = s.frameworkVersionCode,
                api = s.apiVersion,
                scope = s.scope.toList(),
            )
        } catch (t: Throwable) {
            null
        }
    }

    /** Tüm ayarları framework'ün uzak tercihlerine yazar. */
    fun pushAll() {
        val s = service ?: return
        val st = settings ?: return
        try {
            val e = s.getRemotePreferences(Keys.GROUP).edit()
            e.clear()
            e.putStringSet(Keys.APPS, HashSet(st.loadSelectedApps()))
            e.putString(Keys.DEFAULT_COUNTRY, st.defaultCountry)
            st.loadAppCountries().forEach { (pkg, code) -> e.putString(Keys.countryKey(pkg), code) }
            e.putBoolean(Keys.LAYER_LOCATION, st.layerLocation)
            e.putBoolean(Keys.LAYER_SIM, st.layerSim)
            e.putBoolean(Keys.LAYER_TZ, st.layerTimezone)
            e.putBoolean(Keys.LAYER_LOCALE, st.layerLocale)
            e.putBoolean(Keys.LAYER_VPNHIDE, st.layerVpnHide)
            e.putBoolean(Keys.HOOK_LOG, st.hookLog)
            e.apply()
        } catch (t: Throwable) {
            // Sessizce geç: servis o sırada kopmuş olabilir
        }
    }

    /** Henüz kapsamda olmayan uygulamalar için LSPosed'dan onay ister. */
    fun requestScope(ctx: Context, packages: List<String>) {
        val s = service ?: return
        val current = info?.scope ?: emptyList()
        val missing = packages.filter { it !in current }
        if (missing.isEmpty()) return
        val app = ctx.applicationContext
        try {
            s.requestScope(missing, object : XposedService.OnScopeEventListener {
                override fun onScopeRequestApproved(approved: List<String>) {
                    main.post {
                        Toast.makeText(
                            app,
                            app.getString(R.string.scope_approved, approved.size),
                            Toast.LENGTH_SHORT,
                        ).show()
                        refreshInfo(s)
                    }
                }

                override fun onScopeRequestFailed(message: String) {
                    main.post {
                        Toast.makeText(
                            app,
                            app.getString(R.string.scope_failed, message),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
            })
        } catch (t: Throwable) {
            // yoksay
        }
    }
}
