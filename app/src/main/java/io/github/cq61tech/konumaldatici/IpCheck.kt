package io.github.cq61tech.konumaldatici

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Bu uygulamanın çıkış IP'sini sorgular. VPN bağlıyken bu uygulama da tünelde olduğu için
 * burada görünen IP, tünele alınan diğer uygulamaların gördüğü IP ile aynıdır.
 * Sadece kullanıcı "Kontrol et"e basınca ipwho.is adresine bir istek gider.
 */
object IpCheck {
    var text by mutableStateOf<String?>(null)
        private set

    var busy by mutableStateOf(false)
        private set

    suspend fun run() {
        busy = true
        val r = withContext(Dispatchers.IO) {
            try {
                val c = URL("https://ipwho.is/").openConnection() as HttpURLConnection
                c.connectTimeout = 8000
                c.readTimeout = 8000
                val json = JSONObject(c.inputStream.bufferedReader().use { it.readText() })
                val ip = json.optString("ip")
                val cc = json.optString("country_code")
                if (ip.isEmpty()) null else "$ip · $cc"
            } catch (t: Throwable) {
                null
            }
        }
        text = r ?: "—"
        busy = false
    }
}
