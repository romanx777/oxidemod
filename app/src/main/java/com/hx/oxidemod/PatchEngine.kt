package com.hx.oxidemod

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object PatchEngine {
    @Volatile var playersNearby: Int = 0
    @Volatile var closestPlayerDist: Int = 0

    // Повертаємо блок visuals, щоб меню знову працювало
    object visuals {
        @Volatile var box = false
        @Volatile var fovEnabled = false
        @Volatile var fovRadius = 200f
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
        if (pid <= 0) return
        if (moduleBase == 0L) {
            moduleBase = MemAccess.moduleBase(pid, Config.MODULE)
            if (moduleBase == 0L) return
        }

        var listPtr = MemAccess.readLong(pid, moduleBase + Config.kPlayerListBase) ?: return
        if (listPtr == 0L) return
        
        val count = MemAccess.readInt(pid, listPtr + 0x20) ?: return
        if (count <= 0 || count > 100) return
        playersNearby = count

        val itemsPtr = MemAccess.readLong(pid, listPtr + 0x10) ?: return
        if (itemsPtr == 0L) return

        // Беремо позицію першого гравця (тебе)
        val player0Ptr = MemAccess.readLong(pid, itemsPtr) ?: return
        val pos0Bytes = RootHelper.instance.read(pid, player0Ptr + Config.kPositionOff, 12) ?: return
        val buf0 = ByteBuffer.wrap(pos0Bytes).order(ByteOrder.LITTLE_ENDIAN)
        val myX = buf0.float; val myY = buf0.float; val myZ = buf0.float

        var minDist = 9999.0f

        // Рахуємо дистанцію до всіх інших
        for (i in 1 until count) {
            val pPtr = MemAccess.readLong(pid, itemsPtr + (i * 8L)) ?: continue
            if (pPtr == 0L) continue
            
            val pBytes = RootHelper.instance.read(pid, pPtr + Config.kPositionOff, 12) ?: continue
            val pBuf = ByteBuffer.wrap(pBytes).order(ByteOrder.LITTLE_ENDIAN)
            val ex = pBuf.float; val ey = pBuf.float; val ez = pBuf.float
            
            val dist = sqrt((myX - ex) * (myX - ex) + (myY - ey) * (myY - ey) + (myZ - ez) * (myZ - ez))
            if (dist < minDist) minDist = dist
        }
        
        closestPlayerDist = if (minDist == 9999.0f) 0 else minDist.toInt()
    }
}
