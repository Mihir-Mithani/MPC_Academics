package com.example.firebasechat

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.runtime.Composable
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.signInAnonymously
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private var auth: FirebaseAuth? = null
    private var database: DatabaseReference? = null
    private var messagesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Firebase
        com.google.firebase.ktx.Firebase.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("messages")

        setContent {
            FirebaseChatTheme {
                ChatScreen(
                    onSendClick = { text -> sendMessage(text) },
                    onAuthClick = { signInAnonymously() },
                    onSignOutClick = { signOut() }
                )
            }
        }

        // Check auth state
        auth?.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            ChatScreen.updateAuthState(user != null, user?.uid ?: "")
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
            ?.addOnSuccessListener { result ->
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
                ChatScreen.updateMessages(messages)
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
        private var isSignedIn = false
        private var currentUserId = ""
        private var chatMessages = mutableListOf<ChatMessage>()

        fun updateAuthState(signedIn: Boolean, userId: String) {
            isSignedIn = signedIn
            currentUserId = userId
        }

        fun getAuthState(): Pair<Boolean, String> = Pair(isSignedIn, currentUserId)

        fun updateMessages(messages: List<ChatMessage>) {
            chatMessages.clear()
            chatMessages.addAll(messages.sortedBy { it.timestamp })
        }

        fun getMessages(): List<ChatMessage> = chatMessages.toList()
    }
}

@Composable
fun ChatScreen(
    onSendClick: (String) -> Unit,
    onAuthClick: () -> Unit,
    onSignOutClick: () -> Unit
) {
    var (isSignedIn, currentUserId) by remember { mutableStateOf(MainActivity.getAuthState()) }
    var messages by remember { mutableStateOf(MainActivity.getMessages()) }
    var messageText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            val authState = MainActivity.getAuthState()
            isSignedIn = authState.first
            currentUserId = authState.second
            messages = MainActivity.getMessages()
            androidx.compose.runtime.delay(500)
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
                        text = "Firebase Real-time Chat",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    if (isSignedIn) {
                        androidx.compose.material3.Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = "User", tint = Color.Green)
                            Text(text = "User: ${currentUserId.take(8)}", fontSize = 12.sp, color = Color.Green)
                            Button(onClick = onSignOutClick) {
                                Icon(Icons.Filled.Logout, contentDescription = "Sign out")
                                Text("Sign Out")
                            }
                        }
                    } else {
                        Button(onClick = onAuthClick) {
                            Icon(Icons.Filled.Login, contentDescription = "Sign in")
                            Text("Sign In Anonymously")
                        }
                    }
                }
            }
        }

        // Chat Messages Area
        Card(
            modifier = Modifier.padding(16.dp).fillMaxSize().weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSignedIn) "No messages yet. Start the conversation!" else "Sign in to start chatting",
                        fontSize = 16.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp).fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { message ->
                        val isOwn = message.senderId == currentUserId
                        ChatMessageBubble(message = message, isOwn = isOwn)
                    }
                }
            }
        }

        // Input Area (only when signed in)
        if (isSignedIn) {
            Card(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                androidx.compose.material3.Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                onSendClick(messageText)
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: MainActivity.ChatMessage, isOwn: Boolean) {
    androidx.compose.material3.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isOwn) Arrangement.End else Arrangement.Start
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isOwn)
                    androidx.compose.material3.MaterialTheme.colorScheme.primary
                else
                    androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (!isOwn) {
                    Text(
                        text = message.senderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = message.text,
                    fontSize = 16.sp,
                    color = if (isOwn)
                        androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                    else
                        androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp)),
                    fontSize = 10.sp,
                    color = if (isOwn)
                        androidx.compose.material3.MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else
                        androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth()
                )
            }
        }
    }
}