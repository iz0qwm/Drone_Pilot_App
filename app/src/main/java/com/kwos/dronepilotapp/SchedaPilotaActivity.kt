package com.kwos.dronepilotapp

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.kwos.dronepilotapp.adapters.DroneSchedaAdapter
import com.kwos.dronepilotapp.models.Drone

class SchedaPilotaActivity : AppCompatActivity() {

    private lateinit var imageAvatar: ImageView
    private lateinit var textNomePilota: TextView
    private lateinit var textBio: TextView
    private lateinit var recyclerDroni: RecyclerView
    private lateinit var droneAdapter: DroneSchedaAdapter
    private val droneList = mutableListOf<Drone>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scheda_pilota)


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

        val closeButton: Button = findViewById(R.id.close_schedapilota_button)
        closeButton.setOnClickListener { finish() }

        imageAvatar = findViewById(R.id.imageAvatarBig)
        textNomePilota = findViewById(R.id.textNomePilota)
        textBio = findViewById(R.id.textBioPilota)
        recyclerDroni = findViewById(R.id.recyclerDroniPilota)

        droneAdapter = DroneSchedaAdapter(droneList)
        recyclerDroni.layoutManager = LinearLayoutManager(this)
        recyclerDroni.adapter = droneAdapter

        val uid = intent.getStringExtra("uid")
        if (uid == null) {
            Toast.makeText(this, "UID non valido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(uid)
            .get()
            .addOnSuccessListener { userDoc ->
                textNomePilota.text = userDoc.getString("fullName") ?: "Pilota anonimo"
            }

        db.collection("pilotProfiles").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    textBio.text = doc.getString("bio") ?: ""
                    val avatarUrl = doc.getString("avatarUrl")
                    if (!avatarUrl.isNullOrEmpty()) {
                        Glide.with(this).load(avatarUrl).into(imageAvatar)
                    }
                }
            }

        db.collection("pilotProfiles").document(uid)
            .collection("drones")
            .get()
            .addOnSuccessListener { snapshot ->
                droneList.clear()
                for (doc in snapshot) {
                    val drone = Drone(
                        name = doc.getString("name") ?: "",
                        description = doc.getString("description") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: ""
                    )
                    droneList.add(drone)
                }
                droneAdapter.notifyDataSetChanged()
            }
    }
}
