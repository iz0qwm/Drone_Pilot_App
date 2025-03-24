package com.kwos.dronepilotapp

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
    private lateinit var messagesAdapter: GroupChatAdapter
    private val messagesList = mutableListOf<Message>()
    private val firestore = FirebaseFirestore.getInstance()


    private val messagesRef = FirebaseDatabase.getInstance().reference.child("groupchat")
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_groupchat)

        recyclerView = findViewById(R.id.recyclerViewMessages)
        messageInput = findViewById(R.id.editTextMessage)
        sendButton = findViewById(R.id.buttonSend)

        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        messagesAdapter = GroupChatAdapter(messagesList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@GroupChatActivity)
            adapter = messagesAdapter
        }

        sendButton.setOnClickListener { sendMessage() }

        listenForMessages()
    }

    private fun sendMessage() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(this, "Devi essere loggato per inviare messaggi.\nEsci e rientra dall'App", Toast.LENGTH_SHORT).show()
            return
        }

        val text = messageInput.text.toString().trim()
        if (text.isEmpty() || user == null) return

        val message = mapOf(
            "text" to text,
            "senderId" to user.uid,
            "senderName" to user.displayName,
            "timestamp" to System.currentTimeMillis() // Usa il timestamp come Long per evitare problemi con il server
        )

        messagesRef.push().setValue(message).addOnSuccessListener {
            messageInput.text.clear()
        }.addOnFailureListener {
            Toast.makeText(this, "Errore nell'invio", Toast.LENGTH_SHORT).show()
        }
    }

    private fun listenForMessages() {
        messagesRef.orderByChild("timestamp").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messagesList.clear()
                for (data in snapshot.children) {
                    val msg = data.getValue(Message::class.java)
                    if (msg != null) {
                        // Recupera il nome completo dall'ID utente
                        getUserFullName(msg.senderId) { fullName ->
                            // Aggiorna il nome completo nel messaggio
                            msg.senderName = fullName ?: "Utente sconosciuto"
                            messagesList.add(msg)
                            // Ordina i messaggi esplicitamente per timestamp (se Firebase non li ordina correttamente)
                            messagesList.sortBy { it.timestamp }
                            messagesAdapter.notifyDataSetChanged()
                            recyclerView.scrollToPosition(messagesList.size - 1)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@GroupChatActivity, "Errore nel caricamento messaggi", Toast.LENGTH_SHORT).show()
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

    override fun onStart() {
        super.onStart()
        updateUserOnlineStatus(true) // Imposta lo stato online
    }

    override fun onStop() {
        super.onStop()
        updateUserOnlineStatus(false) // Imposta lo stato offline (opzionale)
    }

}
