package com.kwos.dronepilotapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
//per il padding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.view.View
import android.widget.CheckBox

class ImpostazioniActivity : AppCompatActivity() {

    private lateinit var textEmail: TextView
    private lateinit var editFullName: EditText
    private lateinit var btnSalvaNome: Button
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var checkboxPMR: CheckBox


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_impostazioni)

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

        supportActionBar?.hide()

        textEmail = findViewById(R.id.textEmail)
        editFullName = findViewById(R.id.editFullName)
        btnSalvaNome = findViewById(R.id.btnSalvaNome)
        checkboxPMR = findViewById(R.id.checkboxPMR)


        val user = auth.currentUser
        if (user != null) {
            textEmail.text = "Email: ${user.email ?: "Non disponibile"}"

            // Recupera il fullName da Firestore e lo mostra
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val fullName = document.getString("fullName") ?: ""
                        editFullName.setText(fullName)
                        val radioPMR = document.getBoolean("radioPMR") ?: false
                        checkboxPMR.isChecked = radioPMR

                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore nel recupero dei dati", Toast.LENGTH_SHORT).show()
                }
        }

        btnSalvaNome.setOnClickListener {
            val nuovoNome = editFullName.text.toString().trim()
            val haRadioPMR = checkboxPMR.isChecked

            val updates = mapOf(
                "fullName" to nuovoNome,
                "radioPMR" to haRadioPMR
            )

            if (nuovoNome.isNotEmpty() && user != null) {
                db.collection("users").document(user.uid)
                    .update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Dati aggiornati!", Toast.LENGTH_SHORT).show()
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
