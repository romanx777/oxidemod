package com.hx.oxidemod

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object PatchEngine {
    @Volatile var playersNearby: Int = 0
    @Volatile var closestPlayerDist: Int = 0
    @Volatile var debugInfo: String = "wait..."

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
        val np = MemAccess.findPid(Config.PACKAGE)
        if (np != pid) { pid = np; moduleBase = 0L }
        if (pid <= 0) { debugInfo = "No PID"; return }
        if (moduleBase == 0L) {
            moduleBase = MemAccess.moduleBase(pid, Config.MODULE)
            if (moduleBase == 0L) { debugInfo = "No Base"; return }
        }

        val listA = MemAccess.readLong(pid, moduleBase + 0x8L) ?: 0L
        val listB = MemAccess.readLong(pid, moduleBase + 0x10L) ?: 0L
        val listC = MemAccess.readLong(pid, moduleBase + 0x0L) ?: 0L

        debugInfo = "0x8: ${java.lang.Long.toHexString(listA)}\n0x10: ${java.lang.Long.toHexString(listB)}\n0x0: ${java.lang.Long.toHexString(listC)}"
        
        // Поки що нічого не читаємо, тільки дивимось на адреси
        playersNearby = 0
        closestPlayerDist = 0
    }
}
