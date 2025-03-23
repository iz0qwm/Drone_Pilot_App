package com.kwos.dronepilotapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ImpostazioniActivity : AppCompatActivity() {

    private lateinit var textEmail: TextView
    private lateinit var editFullName: EditText
    private lateinit var btnSalvaNome: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impostazioni)
        supportActionBar?.hide()

        textEmail = findViewById(R.id.textEmail)
        editFullName = findViewById(R.id.editFullName)
        btnSalvaNome = findViewById(R.id.btnSalvaNome)

        val user = auth.currentUser
        if (user != null) {
            textEmail.text = "Email: ${user.email ?: "Non disponibile"}"

            // Recupera il fullName da Firestore e lo mostra
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName") ?: ""
                        editFullName.setText(fullName)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore nel recupero dei dati", Toast.LENGTH_SHORT).show()
                }
        }

        btnSalvaNome.setOnClickListener {
            val nuovoNome = editFullName.text.toString().trim()
            if (nuovoNome.isNotEmpty() && user != null) {
                db.collection("users").document(user.uid)
                    .update("fullName", nuovoNome)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Nome aggiornato!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Errore nell'aggiornamento", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Inserisci un nome valido", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
