package com.wedgess.luas.domain.model

import java.text.Normalizer
import java.util.Locale

/** Explicit name aliases only: neighbouring stations are never merged using a distance threshold. */
internal object StationNames {
    private val luas = mapOf(
        "abbey st" to "Abbey Street",
        "ballyogan" to "Ballyogan Wood",
        "broadstone" to "Broadstone - University",
        "citywest" to "Citywest Campus",
        "leopardstown" to "Leopardstown Valley",
        "mayor square" to "Mayor Square - NCI",
        "o connell upr" to "O'Connell Upper"
    )
    private val dart = mapOf(
        "connolly" to "Dublin Connolly",
        "bray daly" to "Bray",
        "dun laoghaire mallin" to "Dun Laoghaire",
        "howth junction and donaghmede" to "Howth Junction",
        "sandycove and glasthule" to "Sandycove"
    )

    fun canonical(transport: TransportType, name: String): String =
        (if (transport == TransportType.LUAS) luas else dart)[key(name)] ?: name.trim()

    fun matchKey(transport: TransportType, name: String): String = key(canonical(transport, name))

    fun key(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
        .replace(Regex("[^a-z0-9]+"), " ").trim()
}
