package com.kwos.dronepilotapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DocumentoAlertActivity : AppCompatActivity() {

    private lateinit var alertText: TextView
    private lateinit var ackCheckbox: CheckBox
    private lateinit var openDocsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documento_alert)

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

        alertText = findViewById(R.id.alertText)
        ackCheckbox = findViewById(R.id.ackCheckbox)
        openDocsButton = findViewById(R.id.openDocsButton)

        val documentId = intent.getStringExtra("documentId") ?: return
        val expiryDate = intent.getStringExtra("expiryDate") ?: "Data sconosciuta"
        val documentTitle = intent.getStringExtra("documentTitle") ?: "il tuo documento"
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        alertText.text = "📅 Il documento \"$documentTitle\" scadrà il $expiryDate"

        val okButton = findViewById<Button>(R.id.okButton)
        okButton.setOnClickListener {
            // Se la checkbox è selezionata, aggiorna Firestore
            if (ackCheckbox.isChecked) {
                FirebaseFirestore.getInstance()
                    .collection("pilotProfiles").document(uid)
                    .collection("documents").document(documentId)
                    .update(
                        mapOf(
                            "acknowledgedExpiryAlert" to true,
                            "lastAcknowledgedDays" to calculateDaysRemaining(expiryDate)
                        )
                    )
            }
            finish()
        }


        openDocsButton.setOnClickListener {
            // Se la checkbox è selezionata, aggiorna Firestore
            if (ackCheckbox.isChecked) {
                FirebaseFirestore.getInstance()
                    .collection("pilotProfiles").document(uid)
                    .collection("documents").document(documentId)
                    .update("acknowledgedExpiryAlert", true)
            }
            val intent = Intent(this, ImpostazioniActivity::class.java)
            intent.putExtra("highlight_documents", true)
            startActivity(intent)
            finish()
        }
    }

    private fun calculateDaysRemaining(expiryDateStr: String): Int {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val expiryDate = sdf.parse(expiryDateStr) ?: return Int.MAX_VALUE
        val today = Calendar.getInstance()
        val millisPerDay = 1000 * 60 * 60 * 24
        return ((expiryDate.time - today.timeInMillis + millisPerDay / 2) / millisPerDay).toInt()
    }


}
