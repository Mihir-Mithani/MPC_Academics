package com.example.bluetoothchat

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
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
import androidx.compose.material3.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Send
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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class MainActivity : AppCompatActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothServerSocket: BluetoothServerSocket? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var connectedDevice: BluetoothDevice? = null
    private var connectedThread: ConnectedThread? = null
    private var acceptThread: AcceptThread? = null
    private var scanReceiver: BroadcastReceiver? = null
    private val PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    private val PERMISSION_REQUEST_CODE = 1001
    private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

    // Handler for UI updates from background threads
    private val handler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            MESSAGE_READ -> {
                val readMessage = msg.obj as String
                ChatScreen.addMessage(ChatMessage(readMessage, false))
            }
            MESSAGE_DEVICE_NAME -> {
                val deviceName = msg.getData().getString("device_name")
                connectedDeviceName = deviceName ?: "Connected"
                isConnected = true
                Toast.makeText(this@MainActivity, "Connected to $deviceName", Toast.LENGTH_SHORT).show()
            }
            MESSAGE_TOAST -> {
                Toast.makeText(this@MainActivity, msg.getData().getString("toast"), Toast.LENGTH_SHORT).show()
            }
        }
        true
    }

    companion object {
        private const val MESSAGE_READ = 1
        private const val MESSAGE_DEVICE_NAME = 2
        private const val MESSAGE_TOAST = 3
    }

    // State variables (in real app, use ViewModel)
    var isConnected = false
        private set
    var connectedDeviceName = ""
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        setContent {
            BluetoothChatTheme {
                ChatScreen(
                    bluetoothAdapter = bluetoothAdapter,
                    isConnected = isConnected,
                    connectedDeviceName = connectedDeviceName,
                    onSendClick = { message -> sendMessage(message) },
                    onConnectClick = { device -> connectToDevice(device) },
                    onDisconnectClick = { disconnect() },
                    onScanClick = { startScan() },
                    onPermissionClick = { requestPermissions() },
                    onServerClick = { startServer() }
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
        ChatScreen.clearDevices()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothAdapter?.startDiscovery()
        } else {
            bluetoothAdapter?.startDiscovery()
        }
        Toast.makeText(this, "Scanning for devices...", Toast.LENGTH_SHORT).show()
    }

    private fun startServer() {
        if (acceptThread == null) {
            acceptThread = AcceptThread()
            acceptThread?.start()
            Toast.makeText(this, "Waiting for connection...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (bluetoothSocket != null) {
            disconnect()
        }
        val connectThread = ConnectThread(device)
        connectThread.start()
        Toast.makeText(this, "Connecting to ${device.name}...", Toast.LENGTH_SHORT).show()
    }

    private fun sendMessage(message: String) {
        connectedThread?.write(message.toByteArray())
    }

    private fun disconnect() {
        connectedThread?.cancel()
        connectedThread = null
        bluetoothSocket?.close()
        bluetoothSocket = null
        connectedDevice = null
        isConnected = false
        connectedDeviceName = ""
        Toast.makeText(this, "Disconnected", Toast.LENGTH_SHORT).show()
    }

    private fun registerScanReceiver() {
        if (scanReceiver == null) {
            scanReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    when (intent.action) {
                        BluetoothDevice.ACTION_FOUND -> {
                            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                            device?.let { ChatScreen.addDevice(it) }
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
        disconnect()
        acceptThread?.cancel()
        bluetoothServerSocket?.close()
        bluetoothAdapter?.cancelDiscovery()
    }

    // Thread to accept incoming connections
    inner class AcceptThread : Thread() {
        private var mmServerSocket: BluetoothServerSocket? = null

        init {
            try {
                mmServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("BluetoothChat", MY_UUID)
            } catch (e: IOException) {
                Log.e("BluetoothChat", "Socket listen() failed", e)
            }
        }

        override fun run() {
            var socket: BluetoothSocket? = null
            while (socket == null) {
                try {
                    socket = mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e("BluetoothChat", "Socket accept() failed", e)
                    break
                }
                if (socket != null) {
                    manageConnectedSocket(socket)
                    try {
                        mmServerSocket?.close()
                    } catch (e: IOException) {
                        Log.e("BluetoothChat", "Could not close server socket", e)
                    }
                    break
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e("BluetoothChat", "Could not close server socket", e)
            }
        }
    }

    // Thread to initiate outgoing connection
    inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private var mmSocket: BluetoothSocket? = null

        init {
            try {
                mmSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                Log.e("BluetoothChat", "Socket create() failed", e)
            }
        }

        override fun run() {
            bluetoothAdapter?.cancelDiscovery()
            try {
                mmSocket?.connect()
            } catch (e: IOException) {
                try {
                    mmSocket?.close()
                } catch (e2: IOException) {
                    Log.e("BluetoothChat", "Could not close client socket", e2)
                }
                handler.obtainMessage(MESSAGE_TOAST).also { msg ->
                    msg.getData().putString("toast", "Unable to connect: ${e.message}")
                }.sendToTarget()
                return
            }
            manageConnectedSocket(mmSocket)
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e("BluetoothChat", "Could not close client socket", e)
            }
        }
    }

    // Manage connected socket
    private fun manageConnectedSocket(socket: BluetoothSocket) {
        bluetoothSocket = socket
        connectedThread = ConnectedThread(socket)
        connectedThread?.start()

        val deviceName = socket.remoteDevice.name
        handler.obtainMessage(MESSAGE_DEVICE_NAME).also { msg ->
            msg.getData().putString("device_name", deviceName)
        }.sendToTarget()
    }

    // Thread for handling connected socket I/O
    inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val buffer = ByteArray(1024)

        override fun run() {
            var numBytes: Int
            while (true) {
                try {
                    numBytes = mmInStream.read(buffer)
                    val receivedMessage = String(buffer, 0, numBytes)
                    handler.obtainMessage(MESSAGE_READ, receivedMessage).sendToTarget()
                } catch (e: IOException) {
                    Log.e("BluetoothChat", "Input stream disconnected", e)
                    handler.obtainMessage(MESSAGE_TOAST).also { msg ->
                        msg.getData().putString("toast", "Connection lost")
                    }.sendToTarget()
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e("BluetoothChat", "Error writing to output stream", e)
                handler.obtainMessage(MESSAGE_TOAST).also { msg ->
                    msg.getData().putString("toast", "Failed to send message")
                }.sendToTarget()
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e("BluetoothChat", "Could not close socket", e)
            }
        }
    }

    // Chat message data class
    data class ChatMessage(val text: String, val isSent: Boolean)

    // Static methods for Compose state management
    companion object {
        private val discoveredDevices = mutableListOf<BluetoothDevice>()
        private val chatMessages = mutableListOf<ChatMessage>()

        fun addDevice(device: BluetoothDevice) {
            if (!discoveredDevices.any { it.address == device.address }) {
                discoveredDevices.add(device)
            }
        }

        fun getDevices(): List<BluetoothDevice> = discoveredDevices.toList()

        fun clearDevices() {
            discoveredDevices.clear()
        }

        fun addMessage(message: ChatMessage) {
            chatMessages.add(message)
        }

        fun getMessages(): List<ChatMessage> = chatMessages.toList()

        fun clearMessages() {
            chatMessages.clear()
        }
    }
}

@Composable
fun ChatScreen(
    bluetoothAdapter: BluetoothAdapter?,
    isConnected: Boolean,
    connectedDeviceName: String,
    onSendClick: (String) -> Unit,
    onConnectClick: (BluetoothDevice) -> Unit,
    onDisconnectClick: () -> Unit,
    onScanClick: () -> Unit,
    onPermissionClick: () -> Unit,
    onServerClick: () -> Unit
) {
    var devices by remember { mutableStateOf(MainActivity.getDevices()) }
    var messages by remember { mutableStateOf(MainActivity.getMessages()) }
    var messageText by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    LaunchedEffect(Unit) {
        while (true) {
            messages = MainActivity.getMessages()
            devices = MainActivity.getDevices()
            androidx.compose.runtime.delay(500)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
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
                        text = "Bluetooth Chat",
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
                if (isConnected) {
                    Text(
                        text = "Connected to: $connectedDeviceName",
                        fontSize = 14.sp,
                        color = Color.Green
                    )
                }
            }
        }

        // Chat messages area
        Card(
            modifier = Modifier.padding(16.dp).fillMaxSize().weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                items(messages.reversed()) { message ->
                    androidx.compose.material3.Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.isSent) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.isSent)
                                    androidx.compose.material3.MaterialTheme.colorScheme.primary
                                else
                                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        ) {
                            Text(
                                text = message.text,
                                fontSize = 16.sp,
                                color = if (message.isSent)
                                    androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                                    else
                                    androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Input area
        Card(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                androidx.compose.material3.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        enabled = isConnected,
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                onSendClick(messageText)
                                messageText = ""
                            }
                        },
                        enabled = isConnected && messageText.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }

                if (!isConnected) {
                    androidx.compose.material3.Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = onScanClick) {
                            Text("Scan Devices")
                        }
                        Button(onClick = onServerClick) {
                            Text("Start Server")
                        }
                        Button(onClick = onPermissionClick) {
                            Text("Permissions")
                        }
                    }
                } else {
                    Button(
                        onClick = onDisconnectClick,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text("Disconnect", color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Device list (when not connected)
        if (!isConnected && devices.isNotEmpty()) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth().height(200.dp),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Available Devices (tap to connect):",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(8.dp).fillMaxWidth()
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(devices) { device ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            ) {
                                androidx.compose.material3.Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .clickable { onConnectClick(device) },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = device.name ?: "Unknown Device",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = device.address,
                                            fontSize = 12.sp,
                                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = if (device.bondState == BluetoothDevice.BOND_BONDED) "Paired" else "Available",
                                        fontSize = 12.sp,
                                        color = if (device.bondState == BluetoothDevice.BOND_BONDED) Color.Green else Color.Orange
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