package com.hx.oxidemod

import java.nio.ByteBuffer
import java.nio.ByteOrder

object MemAccess {
    fun findPid(pkg: String): Int = try {
        val p = ProcessBuilder("su", "-c", "pidof $pkg").start()
        val out = p.inputStream.bufferedReader().readText().trim()
        p.waitFor()
        out.split(' ').firstOrNull()?.toIntOrNull() ?: 0
    } catch (_: Throwable) { 0 }

    fun moduleBase(pid: Int, module: String): Long = try {
        val p = ProcessBuilder("su", "-c", "cat /proc/$pid/maps").start()
        val lines = p.inputStream.bufferedReader().readLines()
        p.waitFor()
        var found = 0L
        for (line in lines) {
            if (line.endsWith(module)) { found = line.substringBefore('-').toLong(16); break }
        }
        found
    } catch (_: Throwable) { 0L }

    private fun le(n: Int) = ByteBuffer.allocate(n).order(ByteOrder.LITTLE_ENDIAN)

    fun readInt(pid: Int, a: Long): Int? =
        RootHelper.instance.read(pid, a, 4)?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).int }
    fun writeInt(pid: Int, a: Long, v: Int): Boolean =
        RootHelper.instance.write(pid, a, le(4).putInt(v).array())
    fun readFloat(pid: Int, a: Long): Float? =
        RootHelper.instance.read(pid, a, 4)?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).float }
    fun writeFloat(pid: Int, a: Long, v: Float): Boolean =
        RootHelper.instance.write(pid, a, le(4).putFloat(v).array())
    fun readLong(pid: Int, a: Long): Long? =
        RootHelper.instance.read(pid, a, 8)?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).long }
}
