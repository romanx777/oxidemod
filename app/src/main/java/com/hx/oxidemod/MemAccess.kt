package com.hx.oxidemod

import java.nio.ByteBuffer
import java.nio.ByteOrder

object MemAccess {
    fun findPid(pkg: String): Int = RootHelper.instance.getPid(pkg)
    fun moduleBase(pid: Int, module: String): Long = RootHelper.instance.getBase(pid, module)
    fun readLong(pid: Int, a: Long): Long? = RootHelper.instance.read(pid, a, 8)?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).long }
    fun readInt(pid: Int, a: Long): Int? = RootHelper.instance.read(pid, a, 4)?.let { ByteBuffer.wrap(it).order(ByteOrder.LITTLE_ENDIAN).int }
    fun writeInt(pid: Int, a: Long, v: Int): Boolean = RootHelper.instance.write(pid, a, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array())
}
