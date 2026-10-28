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

    fun sendCmd(cmd: String): String? = synchronized(lock) {
        val i = stdin ?: return null
        val o = stdout ?: return null
        try {
            i.write("$cmd\n"); i.flush()
            return o.readLine()
        } catch (_: Throwable) { null }
    }

    fun read(pid: Int, addr: Long, size: Int): ByteArray? {
        val r = sendCmd("r $pid ${toHexString(addr)} $size") ?: return null
        if (!r.startsWith("OK ")) return null
        return hexToBytes(r.substring(3))
    }

    fun write(pid: Int, addr: Long, data: ByteArray): Boolean {
        return sendCmd("w $pid ${toHexString(addr)} ${bytesToHex(data)}") == "OK"
    }

    fun getPid(pkg: String): Int {
        val r = sendCmd("pid $pkg")
        if (r != null && r.startsWith("OK ")) return r.substring(3).toIntOrNull() ?: 0
        return try {
            val p = Runtime.getRuntime().exec("su -c pidof $pkg").start()
            val out = p.inputStream.bufferedReader().readText().trim()
            p.waitFor()
            out.split(" ").firstOrNull()?.toIntOrNull() ?: 0
        } catch (_: Exception) { 0 }
    }

    fun getBase(pid: Int, module: String): Long {
        val r = sendCmd("base $pid $module") ?: return 0L
        if (!r.startsWith("OK ")) return 0L
        return r.substring(3).toLongOrNull(16) ?: 0L
    }

    companion object {
        val instance = RootHelper()
        fun bytesToHex(b: ByteArray): String {
            val sb = StringBuilder(b.size * 2)
            for (x in b) { val v = x.toInt() and 0xff; sb.append("0123456789abcdef"[v ushr 4]); sb.append("0123456789abcdef"[v and 15]) }
            return sb.toString()
        }
        fun hexToBytes(s: String): ByteArray {
            require(s.length % 2 == 0)
            return ByteArray(s.length / 2) { ((Character.digit(s[it * 2], 16) shl 4) or Character.digit(s[it * 2 + 1], 16)).toByte() }
        }
    }
}
