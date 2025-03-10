package com.kwos.dronepilotapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

class ChatActivity : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var messagesListView: RecyclerView // Assicurati che sia RecyclerView
    private lateinit var messagesAdapter: ChatAdapter
    private lateinit var receiverId: String
    private lateinit var chattingWithText: TextView // TextView per il nome del pilota

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        supportActionBar?.hide()

        Log.d("ChatActivity", "Activity started")

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        receiverId = intent.getStringExtra("receiverId") ?: ""

        // Recupera la TextView per il nome del pilota
        chattingWithText = findViewById(R.id.chattingWithText)

        // Recupera il nome del pilota con cui stai chattando
        db.collection("users").document(receiverId).get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") // Assicurati che il nome sia nel campo "name"
                if (name != null) {
                    chattingWithText.text = "Stai chattando con: $name"
                }
            }

        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        messagesListView = findViewById(R.id.messagesListView) // RecyclerView
        messagesAdapter = ChatAdapter(this, mutableListOf())

        // Imposta il LinearLayoutManager per il RecyclerView
        messagesListView.layoutManager = LinearLayoutManager(this)
        messagesListView.adapter = messagesAdapter

        sendButton.setOnClickListener { sendMessage() }
        listenForMessages()
    }

    private fun sendMessage() {
        val senderId = auth.currentUser?.uid ?: return
        val messageText = messageInput.text.toString().trim()
        if (messageText.isEmpty()) return

        val message = ChatMessage(senderId, receiverId, messageText)
        db.collection("chats")
            .add(message)
            .addOnSuccessListener { messageInput.text.clear() }
    }

    private fun listenForMessages() {
        val senderId = auth.currentUser?.uid ?: return
        db.collection("chats")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, _ ->
                snapshots?.let {
                    val messages = it.toObjects(ChatMessage::class.java)
                        .filter { msg ->
                            (msg.senderId == senderId && msg.receiverId == receiverId) ||
                                    (msg.senderId == receiverId && msg.receiverId == senderId)
                        }
                    messagesAdapter.updateMessages(messages)
                }
            }
    }
}
