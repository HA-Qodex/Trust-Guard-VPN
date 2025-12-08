package com.my.trustguard.vpn.viewmodels

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.my.trustguard.vpn.models.VpnStats
import com.my.trustguard.vpn.models.VpnUiState
import com.my.trustguard.vpn.repositories.VpnRepository
import com.my.trustguard.vpn.services.WireGuardVpnService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VpnViewModel @Inject constructor(
    private val vpnRepository: VpnRepository
) : ViewModel() {

    //    private val _uiState = MutableStateFlow(VpnUiState())
    val uiState: StateFlow<VpnUiState> = vpnRepository.uiState.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VpnUiState()
    )

    val vpnState: StateFlow<VpnStats> = vpnRepository.vpnStat.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        VpnStats()
    )

    init {
        observeConfig()
    }

    private fun observeConfig() {
        vpnRepository.configFlow
            .onEach { config ->
                vpnRepository.updateState(
                    uiState.value.copy(configContent = config)
                )
            }
            .launchIn(viewModelScope)
    }

    fun loadConfig(configText: String) {
        viewModelScope.launch {
            try {
                vpnRepository.updateState(
                    uiState.value.copy(isLoading = true, error = null)
                )
                if (configText.isNotEmpty()) {
                    vpnRepository.saveConfig(configText)
                    vpnRepository.updateState(
                        uiState.value.copy(
                            configContent = configText,
                            statusMessage = "Config loaded successfully",
                            isLoading = false
                        )
                    )

                } else {
                    vpnRepository.updateState(
                        uiState.value.copy(

                            error = "Config is empty",
                            isLoading = false
                        )
                    )
                }
            } catch (e: Exception) {
                vpnRepository.updateState(
                    uiState.value.copy(
                        error = "Error: ${e.message}",
                        isLoading = false
                    )
                )
            }
        }
    }

    fun checkVPNPermission(context: Context) {
        val activity = context as Activity
        val intent = VpnService.prepare(activity)

        if (intent != null) {
            intent.putExtra("config", uiState.value.configContent)
            activity.startActivityForResult(intent, 1234)
        } else {
            if (uiState.value.isConnected) {
                val intent = Intent(context, WireGuardVpnService::class.java).apply {
                    action = WireGuardVpnService.ACTION_STOP
                }
                context.startService(intent)
            } else {
                val intent = Intent(context, WireGuardVpnService::class.java).apply {
                    action = WireGuardVpnService.ACTION_START
                    putExtra("config", uiState.value.configContent)
                }
                context.startForegroundService(intent)
            }
        }
    }
}
