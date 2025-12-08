package com.my.trustguard.vpn

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.my.trustguard.vpn.screens.VPNMainScreen
import com.my.trustguard.vpn.services.WireGuardVpnService
import com.my.trustguard.vpn.ui.theme.TrustGuardVPNTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrustGuardVPNTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VPNMainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        if(requestCode == 1234 && resultCode == RESULT_OK){
            val intent = Intent(this, WireGuardVpnService::class.java).apply {
                action = WireGuardVpnService.ACTION_START
                putExtra("config", data?.getStringExtra("config"))
            }
            startForegroundService(intent)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TrustGuardVPNTheme {
        Greeting("Android")
    }
}