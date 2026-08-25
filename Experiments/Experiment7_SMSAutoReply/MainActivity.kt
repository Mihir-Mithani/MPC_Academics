package com.example.smsautoreply

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private var smsReceiver: SmsBroadcastReceiver? = null
    private val SMS_PERMISSIONS = arrayOf(
        Manifest.permission.RECEIVE_SMS,
        Manifest.permission.READ_SMS,
        Manifest.permission.SEND_SMS
    )
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SMSAutoReplyTheme {
                SMSAutoReplyScreen(onPermissionClick = { requestSmsPermissions() })
            }
        }
        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        registerSmsReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterSmsReceiver()
    }

    private fun checkAndRequestPermissions() {
        val allGranted = SMS_PERMISSIONS.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
        if (!allGranted) {
            ActivityCompat.requestPermissions(this, SMS_PERMISSIONS, PERMISSION_REQUEST_CODE)
        } else {
            registerSmsReceiver()
        }
    }

    private fun requestSmsPermissions() {
        ActivityCompat.requestPermissions(this, SMS_PERMISSIONS, PERMISSION_REQUEST_CODE)
    }

    private fun registerSmsReceiver() {
        if (smsReceiver == null) {
            smsReceiver = SmsBroadcastReceiver()
            val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
            filter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(smsReceiver, filter, RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(smsReceiver, filter)
            }
        }
    }

    private fun unregisterSmsReceiver() {
        smsReceiver?.let { unregisterReceiver(it) }
        smsReceiver = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                registerSmsReceiver()
                Toast.makeText(this, "SMS permissions granted. Auto-reply enabled.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS permissions denied. Auto-reply won't work.", Toast.LENGTH_LONG).show()
            }
        }
    }

    inner class SmsBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                for (smsMessage in messages) {
                    val sender = smsMessage.originatingAddress
                    val body = smsMessage.messageBody

                    // Send auto-reply
                    sendAutoReply(sender!!, "Thanks for your message! I'm currently busy. I'll get back to you soon. - Auto Reply")

                    // Show toast notification
                    runOnUiThread {
                        Toast.makeText(
                            context,
                            "Auto-reply sent to $sender",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        private fun sendAutoReply(phoneNumber: String, message: String) {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

@Composable
fun SMSAutoReplyScreen(onPermissionClick: () -> Unit) {
    var isAutoReplyEnabled by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SMS Auto-Reply",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

                Text(
                    text = "Enable auto-reply to automatically respond to incoming SMS messages.",
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(24.dp))

                androidx.compose.material3.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto-Reply Status",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Switch(
                        checked = isAutoReplyEnabled,
                        onCheckedChange = { enabled ->
                            isAutoReplyEnabled = enabled
                            Toast.makeText(
                                context,
                                if (enabled) "Auto-reply enabled" else "Auto-reply disabled",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

                Text(
                    text = "Default Reply: \"Thanks for your message! I'm currently busy. I'll get back to you soon. - Auto Reply\"",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))

        Button(
            onClick = onPermissionClick,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Grant SMS Permissions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}