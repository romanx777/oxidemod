package com.hx.oxidemod

import java.io.BufferedReader
import java.io.BufferedWriter
import java.lang.Long.toHexString

class RootHelper private constructor() {
    private var proc: Process? = null
    private var stdin: BufferedWriter? = null
    private var stdout: BufferedReader? = null
    private val lock = Any()

    fun start(helperPath: String): Boolean = synchronized(lock) {
        stop()
        try {
            val p = ProcessBuilder("su", "-c", helperPath).redirectErrorStream(true).start()
            proc = p
            stdin = p.outputStream.bufferedWriter()
            stdout = p.inputStream.bufferedReader()
            true
        } catch (_: Throwable) { false }
    }

    fun stop() = synchronized(lock) {
        try { stdin?.close() } catch (_: Throwable) {}
        try { proc?.destroy() } catch (_: Throwable) {}
        proc = null; stdin = null; stdout = null
    }

    fun isRunning(): Boolean = proc?.isAlive == true

    fun read(pid: Int, addr: Long, size: Int): ByteArray? = synchronized(lock) {
        val i = stdin ?: return null; val o = stdout ?: return null
        try {
            i.write("r $pid ${toHexString(addr)} $size\n"); i.flush()
            val r = o.readLine() ?: return null
            if (!r.startsWith("OK ")) return null
            hexToBytes(r.substring(3))
        } catch (_: Throwable) { null }
    }

    fun write(pid: Int, addr: Long, data: ByteArray): Boolean = synchronized(lock) {
        val i = stdin ?: return false; val o = stdout ?: return false
        try {
            i.write("w $pid ${toHexString(addr)} ${bytesToHex(data)}\n"); i.flush()
            o.readLine() == "OK"
        } catch (_: Throwable) { false }
    }

    companion object {
        val instance = RootHelper()
        fun bytesToHex(b: ByteArray): String {
            val sb = StringBuilder(b.size * 2)
            for (x in b) {
                val v = x.toInt() and 0xff
                sb.append("0123456789abcdef"[v ushr 4])
                sb.append("0123456789abcdef"[v and 15])
            }
            return sb.toString()
        }
        fun hexToBytes(s: String): ByteArray {
            require(s.length % 2 == 0)
            return ByteArray(s.length / 2) {
                ((Character.digit(s[it * 2], 16) shl 4) or Character.digit(s[it * 2 + 1], 16)).toByte()
            }
        }
    }
}
