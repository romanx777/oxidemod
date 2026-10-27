package com.hx.oxidemod

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")))
            Toast.makeText(this, "дозволь overlay, потім повернись", Toast.LENGTH_LONG).show()
            return
        }
        if (!deployHelper()) { Toast.makeText(this, "helper deploy failed (root?)", Toast.LENGTH_LONG).show(); return }
        if (!RootHelper.instance.start("/data/local/tmp/hx_helper")) {
            Toast.makeText(this, "su start failed", Toast.LENGTH_LONG).show(); return
        }
        startForegroundService(Intent(this, OverlayService::class.java))
        Toast.makeText(this, "running", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deployHelper(): Boolean = try {
        val tmp = File(cacheDir, "hx_helper")
        assets.open("helper").use { it.copyTo(tmp.outputStream()) }
        val p = ProcessBuilder("su", "-c",
            "cp ${tmp.absolutePath} /data/local/tmp/hx_helper && " +
            "chmod 755 /data/local/tmp/hx_helper && echo OK")
            .redirectErrorStream(true).start()
        val out = p.inputStream.bufferedReader().readText(); p.waitFor()
        out.contains("OK")
    } catch (_: Throwable) { false }
}
