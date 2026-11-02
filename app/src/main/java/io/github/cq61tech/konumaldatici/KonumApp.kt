package io.github.cq61tech.konumaldatici

import android.app.Application

class KonumApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Cihaz kilidi açılmadan da çalışabilmeli: burada sadece dinleyici kaydedilir.
        ModuleBridge.register()
    }
}
