package com.hx.oxidemod

import java.nio.ByteBuffer
import java.nio.ByteOrder

object PatchEngine {
    @Volatile var playersNearby: Int = 0
    object visuals {
        @Volatile var box = false
        @Volatile var distance = 150f
        @Volatile var fovEnabled = false
        @Volatile var fovRadius = 200f
    }

    data class EnemyData(val screenX: Float, val screenY: Float, val distance: Float)
    @Volatile var enemiesToDraw = mutableListOf<EnemyData>()
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
    fun pidNow(): Int = pid

    private fun worldToScreen(pos: FloatArray, matrix: FloatArray, width: Float, height: Float): Pair<Float, Float>? {
        val w = matrix[3] * pos[0] + matrix[7] * pos[1] + matrix[11] * pos[2] + matrix[15]
        if (w < 0.098f) return null
        val x = matrix[0] * pos[0] + matrix[4] * pos[1] + matrix[8] * pos[2] + matrix[12]
        val y = matrix[1] * pos[0] + matrix[5] * pos[1] + matrix[9] * pos[2] + matrix[13]
        return Pair((width / 2f) * (1f + x / w), (height / 2f) * (1f - y / w))
    }

    private fun tick() {
        if (!visuals.box) return
        val np = MemAccess.findPid(Config.PACKAGE)
        if (np != pid) { pid = np; moduleBase = 0L }
        if (pid <= 0) return
        if (moduleBase == 0L) {
            moduleBase = MemAccess.moduleBase(pid, Config.MODULE)
            if (moduleBase == 0L) return
        }
        if (Config.kPlayerListBase == 0L || Config.kViewMatrixOff == 0L) return

        val matrix = FloatArray(16)
        val matrixBytes = RootHelper.instance.read(pid, moduleBase + Config.kViewMatrixOff, 64) ?: return
        val buf = ByteBuffer.wrap(matrixBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 16) matrix[i] = buf.float

        var listPtr = MemAccess.readLong(pid, moduleBase + Config.kPlayerListBase) ?: return
        if (listPtr == 0L) return
        val count = MemAccess.readInt(pid, listPtr + 0x20) ?: return
        if (count <= 0 || count > 100) return
        playersNearby = count

        val itemsPtr = MemAccess.readLong(pid, listPtr + 0x10) ?: return
        if (itemsPtr == 0L) return

        val tempList = mutableListOf<EnemyData>()
        val width = android.content.res.Resources.getSystem().displayMetrics.widthPixels.toFloat()
        val height = android.content.res.Resources.getSystem().displayMetrics.heightPixels.toFloat()

        for (i in 0 until count) {
            val playerPtr = MemAccess.readLong(pid, itemsPtr + (i * 8L)) ?: continue
            if (playerPtr == 0L) continue
            val observerPtr = MemAccess.readLong(pid, playerPtr + Config.kNetworkObserverOff) ?: continue
            if (observerPtr == 0L) continue
            val posBytes = RootHelper.instance.read(pid, observerPtr + Config.kPositionOff, 12) ?: continue
            val posBuf = ByteBuffer.wrap(posBytes).order(ByteOrder.LITTLE_ENDIAN)
            val pos = floatArrayOf(posBuf.float, posBuf.float, posBuf.float)
            val screenPos = worldToScreen(pos, matrix, width, height)
            if (screenPos != null) tempList.add(EnemyData(screenPos.first, screenPos.second, 0f))
        }
        enemiesToDraw = tempList
    }
}
