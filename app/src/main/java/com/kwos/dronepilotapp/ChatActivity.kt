package com.kwos.dronepilotapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.functions.FirebaseFunctions

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
    private lateinit var messagesListView: RecyclerView
    private lateinit var messagesAdapter: ChatAdapter
    private lateinit var receiverId: String
    private lateinit var chattingWithText: TextView
    private val functions = FirebaseFunctions.getInstance()

    private var userName: String? = null
    private val TAG = "DronePilotApp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        supportActionBar?.hide()

        Log.d(TAG, "Sessione chat attivata")

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        receiverId = intent.getStringExtra("receiverId") ?: ""

        chattingWithText = findViewById(R.id.chattingWithText)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        messagesListView = findViewById(R.id.messagesListView)
        messagesAdapter = ChatAdapter(this, mutableListOf())

        messagesListView.layoutManager = LinearLayoutManager(this)
        messagesListView.adapter = messagesAdapter

        sendButton.setOnClickListener { sendMessage() }

        loadReceiverName()
        listenForMessages()
        clearNewMessageStatus() // Segna i messaggi come "letti"

    }

    private fun loadReceiverName() {
        db.collection("users").document(receiverId).get()
            .addOnSuccessListener { document ->
                userName = document.getString("fullName")
                chattingWithText.text = userName?.let { "Stai chattando con: $it" } ?: "Utente sconosciuto"
            }
            .addOnFailureListener { Log.e(TAG, "Errore nel recupero del nome del pilota", it) }
    }

    private fun sendMessage() {
        val senderId = auth.currentUser?.uid ?: return
        val messageText = messageInput.text.toString().trim()
        if (messageText.isEmpty()) return

        val message = ChatMessage(senderId, receiverId, messageText)
        db.collection("chats")
            .add(message)
            .addOnSuccessListener {
                messageInput.text.clear()
                sendPushNotification(messageText)
            }
            .addOnFailureListener { Log.e(TAG, "Errore nell'invio del messaggio", it) }
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

    private fun sendPushNotification(message: String) {
        val senderId = auth.currentUser?.uid
        if (senderId.isNullOrEmpty()) {
            Log.e(TAG, "Errore: UID del mittente non disponibile.")
            return
        }

        val receiverId = this.receiverId
        if (receiverId.isNullOrEmpty()) {
            Log.e(TAG, "Errore: receiverId mancante.")
            return
        }

        if (message.isEmpty()) {
            Log.e(TAG, "Errore: il messaggio è vuoto.")
            return
        }

        // Log per il debug
        Log.d(TAG, "Invio notifica - receiverId: $receiverId, senderId: $senderId, message: $message")

        // Recupera il FCM token del destinatario
        fetchFCMTokenAndSendNotification(receiverId, message, senderId)
    }

    private fun fetchFCMTokenAndSendNotification(receiverId: String, message: String, senderId: String) {
        val receiverRef = db.collection("users").document(receiverId)

        receiverRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Recupera l'array di token FCM (se esiste)
                val tokens = document.get("fcmTokens") as? List<String>
                Log.d(TAG, "FCM Tokens del receiver: $tokens")

                if (tokens != null && tokens.isNotEmpty()) {
                    // Invia la notifica a ciascun token nell'array
                    tokens.forEach { token ->
                        sendNotificationToReceiver(token, message, senderId)
                        Log.d(TAG, "Invio dopo token - token: $token, senderId: $senderId, message: $message")
                    }
                } else {
                    Log.e(TAG, "Errore: il destinatario non ha token FCM salvati.")
                }
            } else {
                Log.e(TAG, "Errore: Destinatario non trovato.")
            }
        }
    }



    private fun sendNotificationToReceiver(token: String, message: String, senderId: String) {
        val data = hashMapOf(
            "senderId" to senderId,
            "receiverId" to receiverId,
            "message" to message,
            "receiverFcmToken" to token // 🔹 Aggiunto il token FCM del destinatario!
        )

        try {
            functions.getHttpsCallable("sendChatNotification").call(data)
                .addOnSuccessListener {
                    Log.d(TAG, "Notifica push inviata con successo: ${it.data}")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Errore nell'invio della notifica push: ${exception.message}", exception)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Eccezione durante l'invio della notifica push: ${e.message}", e)
        }
    }


    private fun clearNewMessageStatus() {
        val prefs = getSharedPreferences("ChatPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("hasNewMessage", false).apply()
    }

}