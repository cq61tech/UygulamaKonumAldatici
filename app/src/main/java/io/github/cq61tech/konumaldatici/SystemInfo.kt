package io.github.cq61tech.konumaldatici

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/** Aşama 3'te hook motoru kendi sürecinde bunu true yapıp modülün etkin olduğunu bildirecek. */
object ModuleStatus {
    @Volatile
    var active: Boolean = false
}

object SystemInfo {

    fun collect(ctx: Context): List<Pair<Int, String>> {
        val pm = ctx.packageManager
        val unknown = ctx.getString(R.string.val_unknown)
        return listOf(
            R.string.info_android to "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            R.string.info_device to "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
            R.string.info_abi to (Build.SUPPORTED_ABIS.firstOrNull() ?: unknown),
            R.string.info_patch to Build.VERSION.SECURITY_PATCH.ifBlank { unknown },
            R.string.info_kernel to (System.getProperty("os.version") ?: unknown),
            R.string.info_selinux to selinux(ctx),
            R.string.info_root to root(ctx, pm),
            R.string.info_zygisk to ctx.getString(R.string.val_zygisk_hint),
            R.string.info_lsposed to
                if (installed(pm, "org.lsposed.manager")) ctx.getString(R.string.val_found)
                else ctx.getString(R.string.val_not_found),
        )
    }

    private fun installed(pm: PackageManager, pkg: String): Boolean = try {
        pm.getPackageInfo(pkg, 0)
        true
    } catch (e: Exception) {
        false
    }

    private fun selinux(ctx: Context): String = try {
        when (val v = File("/sys/fs/selinux/enforce").readText().trim()) {
            "1" -> "Enforcing"
            "0" -> "Permissive"
            else -> v
        }
    } catch (e: Throwable) {
        ctx.getString(R.string.val_unreadable)
    }

    private fun root(ctx: Context, pm: PackageManager): String {
        val found = buildList {
            if (installed(pm, "me.weishu.kernelsu")) add("KernelSU")
            if (installed(pm, "com.rifsxd.ksunext")) add("KernelSU Next")
            if (installed(pm, "me.bmax.apatch")) add("APatch")
            if (installed(pm, "com.topjohnwu.magisk") || installed(pm, "io.github.huskydg.magisk")) add("Magisk")
        }
        if (found.isNotEmpty()) return found.joinToString(" / ")

        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su", "/su/bin/su",
            "/vendor/bin/su", "/system/sbin/su",
        )
        return if (suPaths.any { File(it).exists() }) ctx.getString(R.string.root_su)
        else ctx.getString(R.string.root_none)
    }
}

fun appVersion(ctx: Context): Pair<String, Long> {
    val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
    return (pi.versionName ?: "?") to pi.longVersionCode
}
