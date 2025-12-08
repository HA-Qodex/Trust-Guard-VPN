package com.my.trustguard.vpn.models

data class VpnUiState(
    var isConnected: Boolean = false,
    val configContent: String = "",
    val statusMessage: String = "Ready",
    val isLoading: Boolean = false,
    val error: String? = null
)