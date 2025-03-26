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
//per il padding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.view.View

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

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        // Recupera la root view del layout
        val rootView = findViewById<View>(android.R.id.content)

        // Applica il padding per evitare che gli elementi vengano coperti
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(
                top = systemBars.top, // Evita sovrapposizione con la status bar
                bottom = systemBars.bottom // Evita sovrapposizione con la navigation bar
            )

            WindowInsetsCompat.CONSUMED
        }
        // fine padding

        logDebug(TAG, "ChatActivity: Sessione chat attivata")

        // Leggo il senderId dall'Intent
        val senderId = intent.getStringExtra("senderId")
        receiverId = intent.getStringExtra("receiverId") ?: ""

        logDebug(TAG, "ChatActivity: senderId ricevuto: ${intent.getStringExtra("senderId")}")
        logDebug(TAG, "ChatActivity: receiverId ricevuto: $receiverId")



        if (senderId.isNullOrBlank()) {
            logError(TAG, "ChatActivity: Errore: senderId è nullo o vuoto!")
        }

        if (receiverId.isBlank()) {
            logError(TAG, "ChatActivity: Errore: receiverId è nullo o vuoto!")
            finish() // Esci dall'activity per evitare errori
            return
        }

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

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
        if (receiverId.isNotBlank()) {
            db.collection("users").document(receiverId).get()
                .addOnSuccessListener { document ->
                    userName = document.getString("fullName")
                    chattingWithText.text = userName?.let { "Stai chattando con: $it" } ?: "Utente sconosciuto"
                }
                .addOnFailureListener { logError(TAG, "ChatActivity: Errore nel recupero del nome del pilota", it) }
        } else {
            logError(TAG, "ChatActivity: Impossibile caricare il nome del pilota: receiverId vuoto")
        }
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
            .addOnFailureListener { logError(TAG, "ChatActivity: Errore nell'invio del messaggio", it) }
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
            logError(TAG, "ChatActivity: Errore: UID del mittente non disponibile.")
            return
        }

        val receiverId = this.receiverId
        if (receiverId.isNullOrEmpty()) {
            logError(TAG, "ChatActivity: Errore: receiverId mancante.")
            return
        }

        if (message.isEmpty()) {
            logError(TAG, "ChatActivity: Errore: il messaggio è vuoto.")
            return
        }

        // Log per il debug
        logDebug(TAG, "ChatActivity: Invio notifica - receiverId: $receiverId, senderId: $senderId, message: $message")

        // Recupera il FCM token del destinatario
        fetchFCMTokenAndSendNotification(receiverId, message, senderId)
    }

    private fun fetchFCMTokenAndSendNotification(receiverId: String, message: String, senderId: String) {
        val receiverRef = db.collection("users").document(receiverId)

        receiverRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Recupera l'array di token FCM (se esiste)
                val tokens = document.get("fcmTokens") as? List<String>
                logDebug(TAG, "ChatActivity: FCM Tokens del receiver: $tokens")

                if (tokens != null && tokens.isNotEmpty()) {
                    // Invia la notifica a ciascun token nell'array
                    tokens.forEach { token ->
                        sendNotificationToReceiver(token, message, senderId)
                        logDebug(TAG, "ChatActivity: Invio dopo token - token: $token, senderId: $senderId, message: $message")
                    }
                } else {
                    logError(TAG, "ChatActivity: Errore: il destinatario non ha token FCM salvati.")
                }
            } else {
                logError(TAG, "ChatActivity: Errore: Destinatario non trovato.")
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
            //logDebug(TAG, "ChatActivity: Invio dati alla funzione: $data")

            functions.getHttpsCallable("sendChatNotification").call(data)
                .addOnSuccessListener {
                    logDebug(TAG, "ChatActivity: Notifica push inviata con successo: ${it.data}")
                }
                .addOnFailureListener { exception ->
                    logError(TAG, "ChatActivity: Errore nell'invio della notifica push: ${exception.message}", exception)
                }
        } catch (e: Exception) {
            logError(TAG, "ChatActivity: Eccezione durante l'invio della notifica push: ${e.message}", e)
        }
    }


    private fun clearNewMessageStatus() {
        val prefs = getSharedPreferences("ChatPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("hasNewMessage", false).apply()
    }

}