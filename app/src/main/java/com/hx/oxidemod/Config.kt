package com.hx.oxidemod

object Config {
    const val PACKAGE = "com.catsbit.oxidesurvivalisland"
    const val MODULE  = "libil2cpp.so"

    // СЮДИ ВПИСУЙ ОФСЕТИ, ЯКЩО ЗНАЙДЕШ (поки нулі - нічого не працює)
    val kLocalPlayerChain = longArrayOf(0x0L) 
    const val kHealthOff  = 0x0L              
    const val kAmmoOff    = 0x0L              
    const val kSpeedOff   = 0x0L              

    const val kHealthValue = 100
    const val kAmmoValue   = 999
    const val kSpeedValue  = 3.0f

    const val TICK_MS        = 50
    const val PID_RECHECK_MS = 2000
}
