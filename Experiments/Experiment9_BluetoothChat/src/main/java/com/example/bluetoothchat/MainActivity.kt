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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@Composable
fun BluetoothChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC5)
        ),
        content = content
    )
}

class MainActivity : ComponentActivity() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val APP_NAME = "BluetoothChat"
    private val MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")

    private var serverThread: AcceptThread? = null
    private var clientThread: ConnectThread? = null
    private var chatService: ConnectedThread? = null

    private val PERMISSIONS = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager?.adapter

        setContent {
            BluetoothChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BluetoothChatScreen(
                        onSendMessage = { sendMessage(it) },
                        onStartServer = { startServer() }
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

    private fun startServer() {
        serverThread = AcceptThread()
        serverThread?.start()
        Toast.makeText(this, "Server started, waiting for connection...", Toast.LENGTH_SHORT).show()
    }

    private fun sendMessage(message: String) {
        if (chatService != null) {
            chatService?.write(message.toByteArray())
            addMessage("Me: $message")
        } else {
            Toast.makeText(this, "Not connected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addMessage(message: String) {
        runOnUiThread {
            chatMessages.add(message)
        }
    }

    companion object {
        val chatMessages = mutableListOf<String>()
    }

    inner class AcceptThread : Thread() {
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return@lazy null
            bluetoothAdapter?.listenUsingInsecureRfcommWithServiceRecord(APP_NAME, MY_UUID)
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }
    }

    inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return@lazy null
            device.createRfcommSocketToServiceRecord(MY_UUID)
        }

        override fun run() {
            if (ActivityCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return
            bluetoothAdapter?.cancelDiscovery()

            mmSocket?.use { socket ->
                socket.connect()
                manageMyConnectedSocket(socket)
            }
        }
    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket) {
        chatService = ConnectedThread(socket)
        chatService?.start()
        addMessage("System: Connected")
    }

    inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024)

        override fun run() {
            var numBytes: Int
            while (true) {
                numBytes = try {
                    mmInStream.read(mmBuffer)
                } catch (e: IOException) {
                    addMessage("System: Connection lost")
                    break
                }
                val message = String(mmBuffer, 0, numBytes)
                addMessage("Remote: $message")
            }
        }

        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                // Error occurred when sending data
            }
        }
    }
}

@Composable
fun BluetoothChatScreen(onSendMessage: (String) -> Unit, onStartServer: () -> Unit) {
    var messages by remember { mutableStateOf(MainActivity.chatMessages.toList()) }
    var textInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            messages = MainActivity.chatMessages.toList()
            delay(500)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Bluetooth Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Button(onClick = onStartServer, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Start Server (Wait for Connection)")
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(messages) { msg ->
                Text(text = msg, modifier = Modifier.padding(vertical = 4.dp))
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") }
            )
            Button(
                onClick = {
                    onSendMessage(textInput)
                    textInput = ""
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}