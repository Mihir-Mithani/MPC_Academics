package com.example.wifisignal

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

@Composable
fun WiFiSignalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC5)
        ),
        content = content
    )
}

class MainActivity : ComponentActivity() {
    private var wifiManager: WifiManager? = null
    private var wifiReceiver: WiFiBroadcastReceiver? = null
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        setContent {
            WiFiSignalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WiFiSignalScreen(
                        onRefreshClick = { updateWiFiInfo() }
                    )
                }
            }
        }
        checkPermissions()
    }

    private fun checkPermissions() {
        if (PERMISSIONS.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, 1001)
        }
    }

    private fun updateWiFiInfo() {
        val wifiInfo = wifiManager?.connectionInfo
        if (wifiInfo != null) {
            currentWiFiInfo = wifiInfo
        }
    }

    companion object {
        var currentWiFiInfo: WifiInfo? = null
    }

    inner class WiFiBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateWiFiInfo()
        }
    }
}

@Composable
fun WiFiSignalScreen(onRefreshClick: () -> Unit) {
    var wifiInfo by remember { mutableStateOf(MainActivity.currentWiFiInfo) }

    LaunchedEffect(Unit) {
        while (true) {
            wifiInfo = MainActivity.currentWiFiInfo
            delay(1000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "WiFi Signal Strength", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onRefreshClick, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text("Refresh Info")
                }
            }
        }

        if (wifiInfo != null && wifiInfo?.networkId != -1) {
            WiFiDetailsCard(wifiInfo!!)
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.padding(start = 16.dp))
                    Text(text = "Not connected to any WiFi network", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }
    }
}

@Composable
fun WiFiDetailsCard(wifiInfo: WifiInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.padding(start = 16.dp))
                Text(text = wifiInfo.ssid ?: "Unknown SSID", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.padding(top = 16.dp))
            DetailRow("Signal Strength", "${wifiInfo.rssi} dBm")
            DetailRow("Link Speed", "${wifiInfo.linkSpeed} Mbps")
            DetailRow("BSSID", wifiInfo.bssid ?: "N/A")
            
            val strength = WifiManager.calculateSignalLevel(wifiInfo.rssi, 5)
            DetailRow("Signal Level", "$strength / 4")
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontWeight = FontWeight.Medium)
        Text(text = value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}