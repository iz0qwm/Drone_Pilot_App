package com.kwos.dronepilotapp

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import android.view.WindowInsets
//per il padding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

class GroupChatActivity : AppCompatActivity() {

    data class Message(
        val senderId: String = "",
        var senderName: String = "",
        val text: String = "",
        val timestamp: Long = 0
    )

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var closeButton: Button
    private lateinit var messagesAdapter: GroupChatAdapter
    private val messagesList = mutableListOf<Message>()
    private val firestore = FirebaseFirestore.getInstance()


    private val messagesRef = FirebaseDatabase.getInstance().reference.child("groupchat")
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groupchat)

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        // Recupera la root view del layout
        //val rootView = findViewById<View>(android.R.id.content)
        val rootView = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)


        // INIZIO PADDING
        // EDGE-TO-EDGE
        // Modalità edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false) // Abilita modalità edge-to-edge

        // Imposta se il contenuto della status bar deve essere scuro (true) o chiaro (false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true // o false, dipende dal tema

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        // FINE EDGE-TO-EDGE

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        // GESTIONE INSETS
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // SOLO paddingBottom per evitare che l'ultima parte vada sotto la navigation bar
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        // fine padding
        // Nasconde la Action Bar
        supportActionBar?.hide()
        // FINE PADDING

        recyclerView = findViewById(R.id.recyclerViewMessages)
        messageInput = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)
        closeButton = findViewById(R.id.close_groupchat_button)

        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        messagesAdapter = GroupChatAdapter(messagesList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GroupChatActivity)
            adapter = messagesAdapter
        }

        sendButton.setOnClickListener { sendMessage() }

        // Pulsante per chiudere la finestra
        closeButton.setOnClickListener {
            finish()
        }

        listenForMessages()
    }

    private fun sendMessage() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Devi essere loggato per inviare messaggi.\nEsci e rientra dall'App", Toast.LENGTH_SHORT).show()
            return
        }

        val text = messageInput.text.toString().trim()
        if (text.isEmpty()) return

        // 🔽 Legge il nome dal Firestore
        FirebaseFirestore.getInstance().collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                val fullName = document.getString("fullName") ?: "Utente"

                val message = mapOf(
                    "text" to text,
                    "senderId" to user.uid,
                    "senderName" to fullName,
                    "timestamp" to System.currentTimeMillis()
                )

                messagesRef.push().setValue(message).addOnSuccessListener {
                    messageInput.text.clear()
                }.addOnFailureListener {
                    Toast.makeText(this, "Errore nell'invio", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun listenForMessages() {
        messagesRef.orderByChild("timestamp").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val msg = snapshot.getValue(Message::class.java)
                if (msg != null) {
                    // Se è un utente Telegram, mantieni il nome già incluso
                    if (msg.senderId.startsWith("telegram_")) {
                        messagesList.add(msg)
                        messagesList.sortBy { it.timestamp }
                        messagesAdapter.notifyItemInserted(messagesList.size - 1)
                        recyclerView.scrollToPosition(messagesList.size - 1)
                    } else {
                        getUserFullName(msg.senderId) { fullName ->
                            msg.senderName = fullName ?: "Utente sconosciuto"
                            messagesList.add(msg)
                            messagesList.sortBy { it.timestamp }
                            messagesAdapter.notifyItemInserted(messagesList.size - 1)
                            recyclerView.scrollToPosition(messagesList.size - 1)
                        }
                    }
                }

            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GroupChatActivity, "Non ci sono messaggi da cancellare", Toast.LENGTH_SHORT).show()
            }
        })
    }


    private fun getUserFullName(userId: String, callback: (String?) -> Unit) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val fullName = document.getString("fullName")
                    callback(fullName)
                } else {
                    callback(null)  // Se non esiste il documento, ritorna null
                }
            }
            .addOnFailureListener {
                callback(null)  // In caso di errore, ritorna null
            }
    }

    private fun updateUserOnlineStatus(isOnline: Boolean) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userRef = FirebaseDatabase.getInstance().reference.child("connectedUsers").child(user.uid)

        if (isOnline) {
            userRef.setValue(true)
            userRef.onDisconnect().removeValue() // Rimuove automaticamente quando l'utente si disconnette
        } else {
            userRef.removeValue()
        }
    }

    companion object {
        var isOpen = false
    }

    override fun onStart() {
        super.onStart()
        updateUserOnlineStatus(true) // Imposta lo stato online
        isOpen = true
        // reset notifica
        DashboardActivity.nuovoMessaggioGruppoPresente = false
        // Fermai il flash
        DashboardActivity.stopFlashingMenuButton()

    }

    override fun onStop() {
        super.onStop()
        updateUserOnlineStatus(false) // Imposta lo stato offline (opzionale)
        isOpen = false
    }

}
