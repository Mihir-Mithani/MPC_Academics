package com.example.wifisignal

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifi3Bar
import androidx.compose.material.icons.filled.SignalWifi2Bar
import androidx.compose.material.icons.filled.SignalWifi1Bar
import androidx.compose.material.icons.filled.SignalWifi0Bar
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private var wifiManager: WifiManager? = null
    private var scanReceiver: BroadcastReceiver? = null
    private val PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    )
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        wifiManager = getSystemService(WifiManager::class.java)

        setContent {
            WiFiSignalTheme {
                WiFiSignalScreen(
                    wifiManager = wifiManager,
                    onScanClick = { startScan() },
                    onPermissionClick = { requestPermissions() },
                    onSettingsClick = { openWifiSettings() }
                )
            }
        }
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        registerScanReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterScanReceiver()
    }

    private fun checkAndRequestPermissions() {
        val allGranted = PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
        } else {
            startScan()
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    private fun startScan() {
        if (wifiManager == null) return

        if (!wifiManager!!.isWifiEnabled) {
            Toast.makeText(this, "Wi-Fi is disabled. Enabling...", Toast.LENGTH_SHORT).show()
            wifiManager!!.isWifiEnabled = true
        }

        WiFiSignalScreen.clearScanResults()
        wifiManager!!.startScan()
        Toast.makeText(this, "Scanning Wi-Fi networks...", Toast.LENGTH_SHORT).show()
    }

    private fun openWifiSettings() {
        val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
        startActivity(intent)
    }

    private fun registerScanReceiver() {
        if (scanReceiver == null) {
            scanReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                            val results = wifiManager?.scanResults
                            results?.let { WiFiSignalScreen.updateScanResults(it) }
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Found ${results?.size ?: 0} networks", Toast.LENGTH_SHORT).show()
                            }
                        }
                        WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                            updateCurrentConnection()
                        }
                        WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                            // Wi-Fi enabled/disabled state changed
                        }
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(scanReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(scanReceiver, filter)
            }
        }
    }

    private fun unregisterScanReceiver() {
        scanReceiver?.let { unregisterReceiver(it) }
        scanReceiver = null
    }

    private fun updateCurrentConnection() {
        val wifiInfo = wifiManager?.connectionInfo
        wifiInfo?.let { WiFiSignalScreen.updateCurrentNetwork(it) }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                startScan()
            }
            Toast.makeText(this, if (allGranted) "Permissions granted" else "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private var scanResults = mutableListOf<ScanResult>()
        private var currentNetwork: WifiInfo? = null

        fun updateScanResults(results: List<ScanResult>) {
            scanResults.clear()
            scanResults.addAll(results.sortedByDescending { it.level })
        }

        fun getScanResults(): List<ScanResult> = scanResults.toList()

        fun clearScanResults() {
            scanResults.clear()
        }

        fun updateCurrentNetwork(info: WifiInfo) {
            currentNetwork = info
        }

        fun getCurrentNetwork(): WifiInfo? = currentNetwork
    }
}

@Composable
fun WiFiSignalScreen(
    wifiManager: WifiManager?,
    onScanClick: () -> Unit,
    onPermissionClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    var scanResults by remember { mutableStateOf(MainActivity.getScanResults()) }
    var currentNetwork by remember { mutableStateOf(MainActivity.getCurrentNetwork()) }
    var isScanning by remember { mutableStateOf(false) }
    var isWifiEnabled by remember { mutableStateOf(wifiManager?.isWifiEnabled == true) }

    LaunchedEffect(Unit) {
        while (true) {
            scanResults = MainActivity.getScanResults()
            currentNetwork = MainActivity.getCurrentNetwork()
            isWifiEnabled = wifiManager?.isWifiEnabled == true
            androidx.compose.runtime.delay(2000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Card
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                androidx.compose.material3.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wi-Fi Signal Strength",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    androidx.compose.material3.Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Filled.Settings, contentDescription = "Wi-Fi Settings")
                        }
                        IconButton(onClick = onPermissionClick) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

                // Current Connected Network
                if (currentNetwork != null && currentNetwork!!.networkId != -1) {
                    val signalLevel = WifiManager.calculateSignalLevel(currentNetwork!!.rssi, 5)
                    androidx.compose.material3.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Connected: ${currentNetwork!!.ssid.replace("\"", "")}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "BSSID: ${currentNetwork!!.bssid} | RSSI: ${currentNetwork!!.rssi} dBm | Link Speed: ${currentNetwork!!.linkSpeed} Mbps",
                                fontSize = 12.sp,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = when (signalLevel) {
                                4 -> Icons.Filled.SignalWifi4Bar
                                3 -> Icons.Filled.SignalWifi3Bar
                                2 -> Icons.Filled.SignalWifi2Bar
                                1 -> Icons.Filled.SignalWifi1Bar
                                else -> Icons.Filled.SignalWifi0Bar
                            },
                            contentDescription = "Signal strength: $signalLevel/4",
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Not connected to any Wi-Fi network",
                        fontSize = 16.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Action Buttons
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            androidx.compose.material3.Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onScanClick, enabled = isWifiEnabled) {
                    if (isScanning) {
                        ProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Filled.Refresh, contentDescription = "Scan")
                    }
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                    Text(text = if (isScanning) "Scanning..." else "Scan Networks", fontSize = 16.sp)
                }
            }
        }

        // Scan Results
        if (scanResults.isEmpty()) {
            Text(
                text = "No networks found. Tap 'Scan Networks' to discover nearby Wi-Fi networks.",
                fontSize = 16.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Available Networks (${scanResults.size}):",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(scanResults) { result ->
                            val signalLevel = WifiManager.calculateSignalLevel(result.level, 5)
                            val isCurrentNetwork = currentNetwork?.bssid == result.BSSID

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentNetwork)
                                        androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                                    else
                                        androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                androidx.compose.material3.Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        androidx.compose.material3.Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = result.SSID.ifEmpty { "\"Hidden Network\"" },
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (isCurrentNetwork) {
                                                Text(
                                                    text = "● CONNECTED",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Text(
                                                text = result.capabilities,
                                                fontSize = 10.sp,
                                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "BSSID: ${result.BSSID} | Freq: ${result.frequency} MHz | RSSI: ${result.level} dBm",
                                            fontSize = 11.sp,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = when (signalLevel) {
                                            4 -> Icons.Filled.SignalWifi4Bar
                                            3 -> Icons.Filled.SignalWifi3Bar
                                            2 -> Icons.Filled.SignalWifi2Bar
                                            1 -> Icons.Filled.SignalWifi1Bar
                                            else -> Icons.Filled.SignalWifi0Bar
                                        },
                                        contentDescription = "Signal: $signalLevel/4",
                                        tint = if (isCurrentNetwork)
                                            androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}