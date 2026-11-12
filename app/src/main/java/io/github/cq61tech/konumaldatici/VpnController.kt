package io.github.cq61tech.konumaldatici

import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.ByteArrayInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object VpnController {
    var state by mutableStateOf(VpnState.Off)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var backend: GoBackend? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val tunnel = object : Tunnel {
        override fun getName(): String = "konum"
        override fun onStateChange(newState: Tunnel.State) {}
    }

    private fun backend(ctx: Context): GoBackend =
        backend ?: GoBackend(ctx.applicationContext).also { backend = it }

    fun syncRunning(ctx: Context) {
        try {
            val running = backend(ctx).runningTunnelNames.isNotEmpty()
            scope.launch { state = if (running) VpnState.On else VpnState.Off }
        } catch (t: Throwable) {
        }
    }

    private fun withApps(text: String, apps: Set<String>): String {
        val cleaned = text.lines()
            .filterNot {
                val k = it.trim().lowercase()
                k.startsWith("includedapplications") || k.startsWith("excludedapplications")
            }
            .joinToString("\n")
        return cleaned.replaceFirst(
            "[Interface]",
            "[Interface]\nIncludedApplications = ${apps.sorted().joinToString(", ")}",
        )
    }

    fun connect(ctx: Context, profile: WgProfile, apps: Set<String>) {
        if (state != VpnState.Off) return
        error = null
        state = VpnState.Connecting
        val app = ctx.applicationContext
        scope.launch {
            val started = SystemClock.elapsedRealtime()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val cfg = Config.parse(
                        ByteArrayInputStream(withApps(profile.text, apps).toByteArray())
                    )
                    backend(app).setState(tunnel, Tunnel.State.UP, cfg)
                }
            }
            val wait = 2500L - (SystemClock.elapsedRealtime() - started)
            if (wait > 0) delay(wait)
            result.onSuccess {
                state = VpnState.On
            }.onFailure {
                error = it.message ?: it.javaClass.simpleName
                state = VpnState.Off
            }
        }
    }

    fun disconnect(ctx: Context) {
        val app = ctx.applicationContext
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { backend(app).setState(tunnel, Tunnel.State.DOWN, null) }
            }
            state = VpnState.Off
        }
    }
}
