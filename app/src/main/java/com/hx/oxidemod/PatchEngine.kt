package com.hx.oxidemod

import android.os.SystemClock

object PatchEngine {
    object visuals {
        @Volatile var box = false
        @Volatile var line = false
        @Volatile var skeleton = false
        @Volatile var name = false
        @Volatile var distance = 150f
    }
    object player {
        @Volatile var health = false
        @Volatile var ammo = false
        @Volatile var speed = 1.0f
    }
    object weapon {
        @Volatile var noRecoil = false
        @Volatile var noSpread = false
    }

    @Volatile private var pid: Int = 0
    @Volatile private var moduleBase: Long = 0L
    @Volatile private var running = false
    private var thread: Thread? = null
    private var lastPidCheck: Long = 0L

    fun start() {
        if (running) return
        running = true
        thread = Thread {
            while (running) {
                try { tick() } catch (_: Throwable) {}
                try { Thread.sleep(Config.TICK_MS.toLong()) } catch (_: InterruptedException) { break }
            }
        }.also { it.isDaemon = true; it.start() }
    }

    fun stop() { running = false; thread?.interrupt(); thread = null }
    fun pidNow(): Int = pid
    fun baseNow(): Long = moduleBase

    private fun tick() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPidCheck > Config.PID_RECHECK_MS) {
            lastPidCheck = now
            val np = MemAccess.findPid(Config.PACKAGE)
            if (np != pid) { pid = np; moduleBase = 0L }
        }
        if (pid <= 0) return
        if (moduleBase == 0L) {
            moduleBase = MemAccess.moduleBase(pid, Config.MODULE)
            if (moduleBase == 0L) return
        }
        
        val chain = Config.kLocalPlayerChain
        if (chain.isEmpty() || chain[0] == 0L) return
        
        var addr = moduleBase + chain[0]
        for (i in 1 until chain.size) {
            val p = MemAccess.readLong(pid, addr) ?: return
            if (p == 0L) return
            addr = p + chain[i]
        }
        val player = addr

        // ЛОГІКА ЗАПИСУ
        if (PatchEngine.player.health) MemAccess.writeInt(pid, player + Config.kHealthOff, Config.kHealthValue)
        if (PatchEngine.player.ammo)   MemAccess.writeInt(pid, player + Config.kAmmoOff,   Config.kAmmoValue)
        
        // Якщо слайдер швидкості не 1.0 - міняємо значення
        if (PatchEngine.player.speed != 1.0f) {
            MemAccess.writeFloat(pid, player + Config.kSpeedOff, PatchEngine.player.speed)
        }
    }
}
