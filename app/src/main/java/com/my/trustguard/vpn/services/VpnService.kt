package com.my.trustguard.vpn.services

import android.content.Context
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val backend: Backend = GoBackend(context)
    private var currentTunnelName: String? = null

    private class TrustGuardTunnel(private val name: String) : Tunnel {
        override fun getName(): String = name
        override fun onStateChange(newState: Tunnel.State) {
            // You can observe tunnel state changes here
        }
    }

    suspend fun setupVPN(configContent: String): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val tunnelName = "trustguard"
            currentTunnelName = tunnelName

            // Parse config
            val config = Config.parse(configContent.byteInputStream())

            // Bring tunnel up
            val tunnel = TrustGuardTunnel(tunnelName)
            backend.setState(tunnel, Tunnel.State.UP, config)

            Result.success("Connected")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopVPN(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            currentTunnelName?.let { tunnelName ->
                val tunnel = TrustGuardTunnel(tunnelName)
                backend.setState(tunnel, Tunnel.State.DOWN, null)
                currentTunnelName = null
            }
            Result.success("Disconnected")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}