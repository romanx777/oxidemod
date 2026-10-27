package com.hx.oxidemod

object Config {
    const val PACKAGE = "com.catsbit.oxidesurvivalisland"
    const val MODULE  = "libil2cpp.so"

    // Твої офсети з дампу
    const val kNetworkObserverOff = 0xA8L 
    const val kPositionOff = 0x18L 
    
    // ТУТ БУДЕ БАЗА СПИСКУ ГРАВЦІВ (поки 0, тому бокси не малюються)
    const val kPlayerListBase = 0x0L 
    const val kViewMatrixOff = 0x0L 

    const val TICK_MS = 30
    const val PID_RECHECK_MS = 2000
}
