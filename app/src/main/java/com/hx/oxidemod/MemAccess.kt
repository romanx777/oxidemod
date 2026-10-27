package com.hx.oxidemod

import java.nio.ByteBuffer
import java.nio.ByteOrder

object MemAccess {
    fun findPid(pkg: String): Int = RootHelper.instance.getPid(pkg)
    fun moduleBase(pid: Int, module: String): Long = RootHelper.instance.getBase(pid, module)

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
