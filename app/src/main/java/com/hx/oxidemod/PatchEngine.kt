package com.hx.oxidemod

import java.nio.ByteBuffer
import java.nio.ByteOrder

object PatchEngine {
    @Volatile var playersNearby: Int = 0
    @Volatile var closestPlayerDist: Int = 0
    @Volatile var debugInfo: String = "searching..."

    object visuals {
        @Volatile var box = false
        @Volatile var fovEnabled = false
        @Volatile var fovRadius = 200f
    }
    object player {
        @Volatile var health = false
        @Volatile var ammo = false
    }

    @Volatile private var pid: Int = 0
    @Volatile private var moduleBase: Long = 0L
    @Volatile private var running = false

    fun start() {
        if (running) return
        running = true
        Thread {
            while (running) {
                try { tick() } catch (_: Throwable) {}
                try { Thread.sleep(Config.TICK_MS.toLong()) } catch (_: InterruptedException) { break }
            }
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop() { running = false }

    private fun tick() {
        val np = RootHelper.instance.getPid(Config.PACKAGE)
        if (np != pid) { pid = np; moduleBase = 0L }
        
        if (pid <= 0) {
            try {
                val p = Runtime.getRuntime().exec("su -c ps -A").start()
                val out = p.inputStream.bufferedReader().readLines()
                val filtered = out.filter { it.contains("catsbit") || it.contains("oxide") || it.contains("unity") }
                debugInfo = if (filtered.isNotEmpty()) "FOUND: ${filtered.first().take(25)}" else "No PID & Not in ps"
            } catch (_: Exception) { debugInfo = "No PID & No Root" }
            return
        }

        if (moduleBase == 0L) {
            moduleBase = RootHelper.instance.getBase(pid, Config.MODULE)
            if (moduleBase == 0L) { debugInfo = "PID: $pid\nNo Base"; return }
        }

        val listA = RootHelper.instance.read(pid, moduleBase + 0x8L, 8)?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).long } ?: 0L
        val listB = RootHelper.instance.read(pid, moduleBase + 0x10L, 8)?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).long } ?: 0L
        val listC = RootHelper.instance.read(pid, moduleBase + 0x0L, 8)?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).long } ?: 0L

        debugInfo = "PID: $pid\n0x8: ${java.lang.Long.toHexString(listA)}\n0x10: ${java.lang.Long.toHexString(listB)}\n0x0: ${java.lang.Long.toHexString(listC)}"
    }
}
