package com.example.bluetoothscanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Refresh
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
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanReceiver: BroadcastReceiver? = null
    private val PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        setContent {
            BluetoothScannerTheme {
                BluetoothScannerScreen(
                    bluetoothAdapter = bluetoothAdapter,
                    onScanClick = { startScan() },
                    onPermissionClick = { requestPermissions() }
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
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    private fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show()
            return
        }

        // Clear previous results (handled in UI state)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothAdapter?.startDiscovery()
        } else {
            bluetoothAdapter?.startDiscovery()
        }
        Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
    }

    private fun registerScanReceiver() {
        if (scanReceiver == null) {
            scanReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                            device?.let { BluetoothScannerScreen.addDevice(it) }
                        }
                        BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Scan complete", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            Toast.makeText(this, if (allGranted) "Permissions granted" else "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothAdapter?.cancelDiscovery()
    }

    companion object {
        // Mutable list to hold discovered devices (in real app, use ViewModel/StateFlow)
        private val discoveredDevices = mutableListOf<BluetoothDevice>()
            .apply { }

        fun addDevice(device: BluetoothDevice) {
            if (!discoveredDevices.any { it.address == device.address }) {
                discoveredDevices.add(device)
            }
        }

        fun getDevices(): List<BluetoothDevice> = discoveredDevices.toList()

        fun clearDevices() {
            discoveredDevices.clear()
        }
    }
}

@Composable
fun BluetoothScannerScreen(
    bluetoothAdapter: BluetoothAdapter?,
    onScanClick: () -> Unit,
    onPermissionClick: () -> Unit
) {
    var devices by remember { mutableStateOf(MainActivity.getDevices()) }
    var isScanning by remember { mutableStateOf(false) }
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    LaunchedEffect(Unit) {
        // Update device list periodically during scan
        while (isScanning) {
            devices = MainActivity.getDevices()
            androidx.compose.runtime.delay(1000)
        }
    }

    DisposableEffect(bluetoothAdapter) {
        val adapter = bluetoothAdapter
        onDispose {
            MainActivity.clearDevices()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bluetooth Device Scanner",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isBluetoothEnabled) Icons.Filled.BluetoothConnected else Icons.Filled.BluetoothDisabled,
                        contentDescription = "Bluetooth status",
                        tint = if (isBluetoothEnabled) Color.Green else Color.Red
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

                androidx.compose.material3.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onScanClick, enabled = isBluetoothEnabled && !isScanning) {
                        if (isScanning) {
                            Icon(Icons.Filled.BluetoothSearching, contentDescription = "Scanning")
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Scan")
                        }
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                        Text(text = if (isScanning) "Scanning..." else "Start Scan", fontSize = 16.sp)
                    }

                    IconButton(onClick = onPermissionClick) {
                        Icon(Icons.Filled.Bluetooth, contentDescription = "Permissions")
                    }
                }
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

        if (devices.isEmpty()) {
            Text(
                text = "No devices found. Tap 'Start Scan' to discover nearby Bluetooth devices.",
                fontSize = 16.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            androidx.compose.material3.Text(
                text = "Found ${devices.size} device(s):",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            )
            LazyColumn(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(devices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        androidx.compose.material3.Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = device.name ?: "Unknown Device",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = device.address,
                                    fontSize = 14.sp,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = when (device.bondState) {
                                    BluetoothDevice.BOND_BONDED -> Icons.Filled.BluetoothConnected
                                    BluetoothDevice.BOND_BONDING -> Icons.Filled.BluetoothSearching
                                    else -> Icons.Filled.Bluetooth
                                },
                                contentDescription = "Bond state",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}