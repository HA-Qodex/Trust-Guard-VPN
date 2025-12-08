package com.my.trustguard.vpn.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.my.trustguard.vpn.services.WireGuardVpnService
import com.my.trustguard.vpn.viewmodels.VpnViewModel

@Composable
fun VPNMainScreen(modifier: Modifier) {
    val viewModel: VpnViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val vpnStats by viewModel.vpnState.collectAsState()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    val shouldRequestPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    }
    val configLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val configText = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader().use { reader -> reader?.readText() ?: "" }
                viewModel.loadConfig(configText)
            } catch (e: Exception) {
                // Error handled by ViewModel
            }
        }
    }

    LaunchedEffect(Unit) {
        if (shouldRequestPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if(uiState.error != null){
        Toast.makeText(context, uiState.error, Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TrustGuard VPN",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 30.dp)
                )
                Text(
                    "Secure Your Connection",
                    fontSize = 14.sp,
                    color = Color(0xFFB0B9C3),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Status Circle
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(
                        color = if (uiState.isConnected) Color(0xFF1E90FF) else Color(0xFF2A3F5F),
                        shape = RoundedCornerShape(90.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text(
                            if (uiState.isConnected) "CONNECTED" else "DISCONNECTED",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        if(uiState.isConnected) vpnStats.lastHandshake.toString() else uiState.statusMessage,
                        fontSize = 12.sp,
                        color = Color(0xFFB0B9C3),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            if (uiState.configContent.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1A2940)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (uiState.isConnected) {
                            Text(
                                "Statistics",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E90FF)
                            )
                            Text(
                                vpnStats.rxBytes.toString(),
                                fontSize = 10.sp,
                                color = Color(0xFFB0B9C3),
                                modifier = Modifier.padding(top = 8.dp),
                                maxLines = 3
                            )
                            Text(
                                vpnStats.txBytes.toString(),
                                fontSize = 10.sp,
                                color = Color(0xFFB0B9C3),
                                modifier = Modifier.padding(top = 8.dp),
                                maxLines = 3
                            )
                            Text(
                                vpnStats.lastHandshake.toString(),
                                fontSize = 10.sp,
                                color = Color(0xFFB0B9C3),
                                modifier = Modifier.padding(top = 8.dp),
                                maxLines = 3
                            )
                            Text(
                                vpnStats.endpoint.toString(),
                                fontSize = 10.sp,
                                color = Color(0xFFB0B9C3),
                                modifier = Modifier.padding(top = 8.dp),
                                maxLines = 3
                            )
                        } else {
                            Text(
                                "Config Loaded",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E90FF)
                            )
                            Text(
                                uiState.configContent.take(100) + if (uiState.configContent.length > 100) "..." else "",
                                fontSize = 10.sp,
                                color = Color(0xFFB0B9C3),
                                modifier = Modifier.padding(top = 8.dp),
                                maxLines = 3
                            )
                        }
                    }
                }
            }

            // Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { configLauncher.launch("*/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF404A60)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !uiState.isLoading
                ) {
                    Text("Load Config File", color = Color.White, fontSize = 16.sp)
                }

                Button(
                    onClick = {
                        viewModel.checkVPNPermission(context)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isConnected) Color(0xFFFF6B6B) else Color(
                            0xFF1E90FF
                        )
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState.configContent.isNotEmpty() && !uiState.isLoading
                ) {
                    Text(
                        if (uiState.isConnected) "Disconnect" else "Connect",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}