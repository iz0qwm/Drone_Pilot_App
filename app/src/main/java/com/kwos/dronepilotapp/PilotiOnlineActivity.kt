package com.kwos.dronepilotapp

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class PilotiOnlineActivity : AppCompatActivity() {

    private lateinit var pilotiRecyclerView: RecyclerView
    private lateinit var pilotiAdapter: PilotiAdapter
    private val pilotiList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_piloti_online)

        supportActionBar?.hide()

        pilotiRecyclerView = findViewById(R.id.recyclerViewPiloti)
        pilotiAdapter = PilotiAdapter(pilotiList)
        pilotiRecyclerView.layoutManager = LinearLayoutManager(this)
        pilotiRecyclerView.adapter = pilotiAdapter

        showPilotiOnline()
    }

    fun showPilotiOnline() {
        // Esegui una query su Firebase per ottenere i piloti connessi e in volo
        val db = FirebaseFirestore.getInstance()
        db.collection("piloti")
            .get()
            .addOnSuccessListener { snapshot ->
                pilotiList.clear() // Pulisce la lista prima di aggiungere i nuovi dati
                for (document in snapshot) {
                    val nomePilota = document.getString("name") ?: "Sconosciuto"
                    val nomeDrone = document.getString("drone") ?: "Drone sconosciuto" // Cambia con il nome del campo del drone
                    val pilotaConDrone = "$nomePilota con $nomeDrone"
                    pilotiList.add(pilotaConDrone)
                }
                pilotiAdapter.notifyDataSetChanged() // Notifica l'adapter che i dati sono cambiati
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Errore: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
