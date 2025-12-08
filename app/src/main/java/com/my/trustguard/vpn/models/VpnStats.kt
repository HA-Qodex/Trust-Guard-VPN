package com.my.trustguard.vpn.models

data class VpnStats(
    val rxBytes: String? = "",
    val txBytes: String? = "",
    val lastHandshake: String? = "",
    val endpoint: String? = ""
)
