package com.example.firebasechat

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.delay

@Composable
fun FirebaseChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6200EE),
            secondary = Color(0xFF03DAC5)
        ),
        content = content
    )
}

class MainActivity : ComponentActivity() {
    private var auth: FirebaseAuth? = null
    private var database: DatabaseReference? = null
    private var messagesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("messages")

        setContent {
            FirebaseChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(
                        onSendClick = { text -> sendMessage(text) },
                        onAuthClick = { signInAnonymously() },
                        onSignOutClick = { signOut() }
                    )
                }
            }
        }

        auth?.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            updateAuthState(user != null, user?.uid ?: "")
            if (user != null) {
                listenForMessages()
            } else {
                removeMessagesListener()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeMessagesListener()
    }

    private fun signInAnonymously() {
        auth?.signInAnonymously()
            ?.addOnSuccessListener {
                Toast.makeText(this, "Signed in anonymously", Toast.LENGTH_SHORT).show()
            }
            ?.addOnFailureListener { e ->
                Toast.makeText(this, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun signOut() {
        auth?.signOut()
        Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
    }

    private fun sendMessage(text: String) {
        val user = auth?.currentUser
        if (user != null && text.isNotBlank()) {
            val message = ChatMessage(
                text = text,
                senderId = user.uid,
                senderName = "User ${user.uid.take(6)}",
                timestamp = System.currentTimeMillis()
            )
            database?.push()?.setValue(message)
        }
    }

    private fun listenForMessages() {
        removeMessagesListener()
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val message = child.getValue(ChatMessage::class.java)
                    message?.let { messages.add(it) }
                }
                updateMessages(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load messages: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        database?.addValueEventListener(messagesListener!!)
    }

    private fun removeMessagesListener() {
        messagesListener?.let { database?.removeEventListener(it) }
        messagesListener = null
    }

    data class ChatMessage(
        val text: String = "",
        val senderId: String = "",
        val senderName: String = "",
        val timestamp: Long = 0
    )

    companion object {
        var isSignedIn by mutableStateOf(false)
        var currentUserId by mutableStateOf("")
        val chatMessages = mutableListOf<ChatMessage>()

        fun updateAuthState(signedIn: Boolean, userId: String) {
            isSignedIn = signedIn
            currentUserId = userId
        }

        fun updateMessages(messages: List<ChatMessage>) {
            chatMessages.clear()
            chatMessages.addAll(messages.sortedBy { it.timestamp })
        }
    }
}

@Composable
fun ChatScreen(
    onSendClick: (String) -> Unit,
    onAuthClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    var messages by remember { mutableStateOf(MainActivity.chatMessages.toList()) }
    var messageText by remember { mutableStateOf("") }

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
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Firebase Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                if (MainActivity.isSignedIn) {
                    Button(onClick = onSignOutClick) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Text("Sign Out")
                    }
                } else {
                    Button(onClick = onAuthClick) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Text("Sign In")
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            items(messages) { message ->
                val isOwn = message.senderId == MainActivity.currentUserId
                ChatMessageBubble(message, isOwn)
            }
        }

        if (MainActivity.isSignedIn) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                Button(
                    onClick = {
                        onSendClick(messageText)
                        messageText = ""
                    },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: MainActivity.ChatMessage, isOwn: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isOwn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
            ),
            modifier = Modifier.fillMaxWidth(0.7f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (!isOwn) {
                    Text(text = message.senderName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Text(text = message.text, color = if (isOwn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}