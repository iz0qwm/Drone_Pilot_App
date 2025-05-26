package com.kwos.dronepilotapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.google.firebase.firestore.FirebaseFirestore
import com.kwos.dronepilotapp.adapters.PilotaOnline
import com.kwos.dronepilotapp.adapters.PilotCardAdapter

class PilotiOnlineActivity : AppCompatActivity() {

    private lateinit var pilotiRecyclerView: RecyclerView
    private lateinit var pilotiAdapter: PilotCardAdapter
    private val pilotiList = mutableListOf<PilotaOnline>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_piloti_online)

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

        val closeButton: Button = findViewById(R.id.close_pilotionline_button)
        closeButton.setOnClickListener { finish() }

        pilotiRecyclerView = findViewById(R.id.recyclerViewPiloti)
        pilotiAdapter = PilotCardAdapter(this, pilotiList)
        pilotiRecyclerView.layoutManager = LinearLayoutManager(this)
        pilotiRecyclerView.adapter = pilotiAdapter

        showPilotiOnline()
    }

    private fun showPilotiOnline() {
        val db = FirebaseFirestore.getInstance()
        pilotiList.clear()

        db.collection("piloti")
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Toast.makeText(this, "Nessun pilota online al momento", Toast.LENGTH_SHORT).show()
                    pilotiAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                var counter = 0
                val total = snapshot.size()

                for (document in snapshot) {
                    val uid = document.id
                    val nomeDrone = document.getString("drone") ?: "Drone sconosciuto"

                    // 🔹 Recupera nome completo da users
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            val nomePilota = userDoc.getString("fullName") ?: "Sconosciuto"

                            // 🔹 Recupera avatar da pilotProfiles (opzionale)
                            db.collection("pilotProfiles").document(uid).get()
                                .addOnSuccessListener { profileDoc ->
                                    val avatarUrl = profileDoc.getString("avatarUrl") ?: ""

                                    val pilota = PilotaOnline(
                                        uid = uid,
                                        name = nomePilota,
                                        droneName = nomeDrone,
                                        avatarUrl = avatarUrl
                                    )
                                    pilotiList.add(pilota)
                                }
                                .addOnFailureListener {
                                    // Se profilo mancante, carica comunque senza avatar
                                    val pilota = PilotaOnline(
                                        uid = uid,
                                        name = nomePilota,
                                        droneName = nomeDrone,
                                        avatarUrl = ""
                                    )
                                    pilotiList.add(pilota)
                                }
                                .addOnCompleteListener {
                                    counter++
                                    if (counter == total) {
                                        pilotiAdapter.notifyDataSetChanged()
                                    }
                                }
                        }
                        .addOnFailureListener {
                            counter++
                            if (counter == total) {
                                pilotiAdapter.notifyDataSetChanged()
                            }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Errore durante il caricamento", Toast.LENGTH_SHORT).show()
            }
    }



}
