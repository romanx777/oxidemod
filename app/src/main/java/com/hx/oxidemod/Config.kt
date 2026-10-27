package com.hx.oxidemod

object Config {
    const val PACKAGE = "com.oxide.game"    // FILL: target package
    const val MODULE  = "libil2cpp.so"      // FILL: main module

    val kLocalPlayerChain = longArrayOf(0x0L) // FILL: [static, ptr1, ..., finalOff]
    const val kHealthOff  = 0x0L              // FILL
    const val kAmmoOff    = 0x0L              // FILL
    const val kSpeedOff   = 0x0L              // FILL

    const val kHealthValue = 100
    const val kAmmoValue   = 999
    const val kSpeedValue  = 3.0f

    const val TICK_MS        = 50
    const val PID_RECHECK_MS = 2000
}
