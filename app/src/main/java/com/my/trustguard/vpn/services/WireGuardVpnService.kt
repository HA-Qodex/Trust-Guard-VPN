package com.my.trustguard.vpn.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import android.util.Log
import androidx.compose.animation.core.copy
import androidx.core.app.NotificationCompat
import com.my.trustguard.vpn.models.VpnStats
import com.my.trustguard.vpn.repositories.VpnRepository
import com.my.trustguard.vpn.utils.AppUtils
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@SuppressLint("VpnServicePolicy")
@AndroidEntryPoint
class WireGuardVpnService : VpnService() {
    @Inject
    lateinit var vpnRepository: VpnRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val backend: Backend by lazy { GoBackend(this) }
    private lateinit var tunnel: Tunnel
    private lateinit var endpoint: String

    companion object {
        const val ACTION_START = "vpn_start"
        const val ACTION_STOP = "vpn_stop"
        private const val NOTIFICATION_ID = 99
        private const val CHANNEL_ID = "vpn_channel"
    }

    private fun startVpn(configContent: String) {
        scope.launch {
            withContext(Dispatchers.Main) {
                vpnRepository.updateState(vpnRepository.uiState.value.copy(isLoading = true))
            }

            val result = runCatching {
                val config = Config.parse(configContent.byteInputStream())
                endpoint = config.peers.firstOrNull()?.endpoint?.get().toString()
                backend.setState(tunnel, Tunnel.State.UP, config)
                "VPN successfully connected"
            }

            result.onSuccess {
                startForegroundServiceNotification()
                withContext(Dispatchers.Main) {
                    vpnRepository.updateState(
                        vpnRepository.uiState.value.copy(isLoading = false, isConnected = true)
                    )
                }
                monitorVpnStats()
            }.onFailure { e ->
                Log.e("WireGuardVpnService", "Failed to start VPN", e)
                withContext(Dispatchers.Main) {
                    vpnRepository.updateState(
                        vpnRepository.uiState.value.copy(
                            isLoading = false,
                            isConnected = false,
                            error = e.stackTrace.toString()
                        )
                    )
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        tunnel = TrustGuardTunnel("trustguard")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
                scope.launch {
                    stopVpnInternal()
                }
                return START_NOT_STICKY
            }

            ACTION_START, null -> {
                val config = intent?.getStringExtra("config")
                if (config != null) {
                    startVpn(config)
                } else {
                    Log.w("WireGuardVpnService", "Start requested without config")
                }
            }
        }
        return START_STICKY
    }

    private suspend fun stopVpnInternal() {
        try {
            backend.setState(tunnel, Tunnel.State.DOWN, null)

            withContext(Dispatchers.Main) {
                vpnRepository.updateState(
                    vpnRepository.uiState.value.copy(isLoading = false, isConnected = false)
                )
            }
        } catch (e: Exception) {
            Log.e("WireGuardVpnService", "Error while stopping VPN", e)
        }
    }

    private fun monitorVpnStats() {
        val currentTime = System.currentTimeMillis()
        scope.launch {
            while (vpnRepository.uiState.value.isConnected) {
                try {
                    val stats = backend.getStatistics(tunnel)
                    val peerPublicKey = stats.peers().firstOrNull()

                    if (peerPublicKey != null) {
                        val peer = stats.peer(peerPublicKey)

                        if (peer != null) {

                            val updatedStats = VpnStats(
                                rxBytes = AppUtils().formatBytes(peer.rxBytes()),
                                txBytes = AppUtils().formatBytes(peer.txBytes()),
                                lastHandshake = AppUtils().formatDuration(peer.latestHandshakeEpochMillis),
                                endpoint = endpoint
                            )

                            withContext(Dispatchers.Main) {
                                vpnRepository.updateVpnState(
                                    updatedStats
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WireGuardVpnService", "Failed to read stats", e)
                }
                delay(1000L)
            }
        }
    }

    private fun startForegroundServiceNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager != null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "TrustGuard VPN connection status"
            }
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, WireGuardVpnService::class.java).apply {
            action = ACTION_STOP
        }

        val requestCode = ACTION_STOP.hashCode()

        val stopPendingIntent = PendingIntent.getService(
            this,
            requestCode,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TrustGuard VPN Connected")
            .setContentText("Your connection is secured with TrustGuard")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .addAction(android.R.drawable.ic_lock_idle_lock, "Disconnect", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

private class TrustGuardTunnel(private val tunnelName: String) : Tunnel {
    override fun getName(): String = tunnelName

    override fun onStateChange(newState: Tunnel.State) {
        Log.i("TrustGuardTunnel", newState.toString())
    }
}