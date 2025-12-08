package com.my.trustguard.vpn.repositories

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.my.trustguard.vpn.models.VpnStats
import com.my.trustguard.vpn.models.VpnUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("vpn_config")

@Singleton
class VpnRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val CONFIG_KEY = stringPreferencesKey("wireguard_config")

    val configFlow: Flow<String> = context.dataStore.data.map { it[CONFIG_KEY] ?: "" }

    private val _uiState = MutableStateFlow(VpnUiState())
    val uiState: StateFlow<VpnUiState> get() = _uiState.asStateFlow()

    private val _vpnStat = MutableStateFlow(VpnStats())
    val vpnStat: StateFlow<VpnStats> get() = _vpnStat.asStateFlow()



    suspend fun saveConfig(config: String) {
        context.dataStore.edit { preferences ->
            preferences[CONFIG_KEY] = config
        }
    }

    fun updateState(uiState: VpnUiState) {
        _uiState.value = uiState
    }

    fun updateVpnState(vpnStats: VpnStats){
        _vpnStat.value = vpnStats
    }
}