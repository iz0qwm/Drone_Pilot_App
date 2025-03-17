package com.kwos.dronepilotapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import android.text.method.LinkMovementMethod
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val TAG = "DronePilotApp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        //Carica il logo
        val logoImage: ImageView = findViewById(R.id.logoImage)
        logoImage.setImageResource(R.drawable.logo)

        //Descrizione sulla pagina iniziale
        val descriptionTextView: TextView = findViewById(R.id.descriptionText)
        descriptionTextView.text = HtmlCompat.fromHtml(getString(R.string.description_text), HtmlCompat.FROM_HTML_MODE_LEGACY)
        descriptionTextView.movementMethod = LinkMovementMethod.getInstance() // Abilita link cliccabili se presenti

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val fullNameField = findViewById<EditText>(R.id.fullNameField)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            googleApiAvailability.makeGooglePlayServicesAvailable(this)
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            loginUser(email, password)
        }

        registerButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val fullName = fullNameField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && fullName.isNotEmpty()) {
                registerUser(email, password, fullName)
            } else {
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            }
        }



    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login riuscito, procedi con l'ottenimento del token FCM
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { tokenTask ->
                        if (tokenTask.isSuccessful) {
                            val token = tokenTask.result
                            Log.d(TAG, "Nuovo Token FCM: $token")
                            saveTokenToServer(token) // Salva il token nel database
                        } else {
                            Log.e(TAG, "Errore nel recuperare il token FCM")
                        }
                    }

                    Toast.makeText(this, "Login riuscito!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Errore: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveTokenToServer(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val userDocRef = db.collection("users").document(userId)

            // Log per vedere se l'utente è effettivamente autenticato
            Log.d(TAG, "Utente autenticato: $userId")

            // Aggiungi il token alla lista esistente di token
            userDocRef.update("fcmTokens", FieldValue.arrayUnion(token))
                .addOnSuccessListener {
                    Log.d(TAG, "Token FCM aggiunto nel database per l'utente $userId")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Errore aggiornando il token FCM", exception)
                }
        } else {
            Log.e(TAG, "Errore: nessun utente autenticato")
        }
    }


    private fun registerUser(email: String, password: String, fullName: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                val userMap = hashMapOf(
                    "uid" to userId,
                    "email" to email,
                    "fullName" to fullName,
                    "availableForChat" to false // Chat disabilitata di default
                )

                userId?.let {
                    db.collection("users").document(it).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registrazione completata!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Register", "Errore durante il salvataggio", e)
                        }
                }
            } else {
                Log.e("Register", "Registrazione fallita", task.exception)
            }
        }
    }

}
