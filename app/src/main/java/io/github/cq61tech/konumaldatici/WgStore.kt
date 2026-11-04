package io.github.cq61tech.konumaldatici

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import java.io.File

/** İçe aktarılmış bir WireGuard profili. Dosya uygulamanın özel klasöründe tutulur. */
class WgProfile(val id: String, val country: String, val text: String)

object WgStore {
    private val PEER_LABEL = Regex("""^#\s*([A-Z]{2})-([A-Za-z]+)#(\d+)""", RegexOption.MULTILINE)
    private val NAME_LABEL = Regex("""([A-Z]{2})-([A-Za-z]+)-(\d+)""")

    private fun dir(ctx: Context): File = File(ctx.filesDir, "wg").also { it.mkdirs() }

    fun list(ctx: Context): List<WgProfile> =
        (dir(ctx).listFiles { f -> f.name.endsWith(".conf") } ?: emptyArray())
            .sortedBy { it.name }
            .mapNotNull { f ->
                val id = f.name.removeSuffix(".conf")
                try {
                    WgProfile(id, id.take(2), f.readText())
                } catch (t: Throwable) {
                    null
                }
            }

    /** Dosyayı doğrular ve kaydeder. Geçersizse null döner. */
    fun import(ctx: Context, uri: Uri): WgProfile? {
        return try {
            val text = ctx.contentResolver.openInputStream(uri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return null
            // Biçimi kütüphaneyle doğrula (geçersizse istisna fırlatır)
            Config.parse(ByteArrayInputStream(text.toByteArray()))

            val fromPeer = PEER_LABEL.find(text)?.destructured?.let { (cc, kind, n) -> "$cc-$kind-$n" }
            val fromName = displayName(ctx, uri)
                ?.let { NAME_LABEL.find(it) }
                ?.destructured
                ?.let { (cc, kind, n) -> "$cc-$kind-$n" }
            val id = (fromPeer ?: fromName ?: "WG-${text.hashCode().toUInt()}").uppercase()

            File(dir(ctx), "$id.conf").writeText(text)
            WgProfile(id, id.take(2), text)
        } catch (t: Throwable) {
            null
        }
    }

    private fun displayName(ctx: Context, uri: Uri): String? = try {
        ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    } catch (t: Throwable) {
        null
    }
}

fun flagOf(countryCode: String): String =
    Countries.all.firstOrNull { it.code == countryCode }?.flag ?: "🌐"
