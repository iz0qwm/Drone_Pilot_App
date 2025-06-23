package com.kwos.dronepilotapp

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class PreFlightChecklistActivity : AppCompatActivity() {

    private lateinit var checklistItems: List<CheckBox>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pre_flight_checklist)

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



        checklistItems = listOf(
            findViewById(R.id.cb_gimbal),
            findViewById(R.id.cb_gimbal_movimenti),
            findViewById(R.id.cb_qrcode),
            findViewById(R.id.cb_batteria),
            findViewById(R.id.cb_meteo),
            findViewById(R.id.cb_eliche),
            findViewById(R.id.cb_gps),
            findViewById(R.id.cb_autorizzazioni),
            findViewById(R.id.cb_assicurazione),
            findViewById(R.id.cb_ostacoli),
            findViewById(R.id.cb_rth)
        )

        val btnConferma = findViewById<Button>(R.id.btn_conferma)
        btnConferma.setOnClickListener {
            val allChecked = checklistItems.all { it.isChecked }
            if (allChecked) {
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Completa tutti i controlli", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
