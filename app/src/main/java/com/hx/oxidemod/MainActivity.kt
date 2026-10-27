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
            try {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                Toast.makeText(this, "Увімкни 'Поверх інших додатків' і повернись", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Дай дозвіл на Overlay в налаштуваннях вручну", Toast.LENGTH_LONG).show()
            }
            return
        }

        Toast.makeText(this, "Запускаю... зачекай", Toast.LENGTH_SHORT).show()

        // Запускаємо у фоні, щоб екран не зависав
        Thread {
            if (!deployHelper()) {
                runOnUiThread { Toast.makeText(this, "Помилка копіювання (потрібен root)", Toast.LENGTH_LONG).show() }
                return@Thread
            }
            if (!RootHelper.instance.start("/data/local/tmp/hx_helper")) {
                runOnUiThread { Toast.makeText(this, "Відмовлено в root", Toast.LENGTH_LONG).show() }
                return@Thread
            }
            runOnUiThread {
                startForegroundService(Intent(this, OverlayService::class.java))
                Toast.makeText(this, "Працює! Шукай кнопку ≡", Toast.LENGTH_SHORT).show()
                finish()
            }
        }.start()
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
