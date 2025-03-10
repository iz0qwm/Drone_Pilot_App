package com.kwos.dronepilotapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var chatToggle: Switch

    private var mapFragment: SupportMapFragment? = null
    private var userName: String? = null  // Ora viene caricato da loadUserName()
    private var droneName: String? = null
    private val TAG = "DashboardActivity"
    private var pilotsListener: ListenerRegistration? = null
    private val pilotMarkers = mutableMapOf<String, Marker>()

    private lateinit var locationCallback: LocationCallback  // Variabile per il callback della posizione

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        db = FirebaseFirestore.getInstance()

        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        chatToggle = findViewById(R.id.chatToggle)
        loadUserName() // Carica il nome del pilota all'avvio

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val startFlightButton = findViewById<Button>(R.id.startFlightButton)
        val stopFlightButton = findViewById<Button>(R.id.stopFlightButton)
        val droneField = findViewById<EditText>(R.id.droneField)
        val mapContainer = findViewById<FrameLayout>(R.id.mapContainer)

        mapContainer.visibility = View.VISIBLE

        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        startFlightButton.setOnClickListener {
            droneName = droneField.text.toString()
            if (!userName.isNullOrEmpty() && !droneName.isNullOrEmpty()) {
                startFlight(userName!!, droneName!!)
            } else {
                Toast.makeText(this, "Caricamento nome pilota in corso o nome drone mancante", Toast.LENGTH_SHORT).show()
            }
        }

        stopFlightButton.setOnClickListener {
            val currentUserName = userName ?: ""  // Evita il nullable
            if (currentUserName.isNotEmpty()) {
                Log.d("DronePilotApp", "Tentativo di eliminare il volo per $currentUserName")
                stopFlight(currentUserName)
            } else {
                Toast.makeText(this, "Non posso fermare un volo inesistente", Toast.LENGTH_SHORT).show()
            }
        }

        // Recupera lo stato della chat da Firestore al login
        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        chatToggle.isChecked = document.getBoolean("availableForChat") ?: false
                    }
                }
        }
        // Ascolta le modifiche del toggle e aggiorna Firestore
        chatToggle.setOnCheckedChangeListener { _, isChecked ->
            auth.currentUser?.uid?.let { userId ->
                db.collection("users").document(userId)
                    .update("availableForChat", isChecked)
                    .addOnSuccessListener {
                        Log.d("ChatToggle", "Stato chat aggiornato: $isChecked")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ChatToggle", "Errore nell'aggiornamento", e)
                    }
            }
        }

        showMap()
    }

    private fun loadUserName() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userName = document.getString("fullName") // Salva il nome per riutilizzarlo
                        findViewById<TextView>(R.id.welcomeTextView).text = "Benvenuto, $userName!"
                    }
                }
                .addOnFailureListener {
                    Log.e("Dashboard", "Errore nel recupero del nome", it)
                }
        }
    }

    private fun showMap() {
        mapFragment = supportFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.mapContainer, mapFragment!!)
                .commitNow()
        }
        mapFragment!!.getMapAsync(this)
    }

    private fun openChatWithPilot(userId: String) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("receiverId", userId)  // Assicurati di usare la chiave giusta
        startActivity(intent)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        // Imposta il listener per il click sulla stringa
        mMap.setOnInfoWindowClickListener { marker ->
            val userId = marker.tag as? String  // Recupera l'ID del pilota
            if (userId != null) {
                // Avvia la chat con il pilota usando l'ID
                openChatWithPilot(userId)
            }
        }


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            mMap.isMyLocationEnabled = true
            loadPilots()
        }
    }


    private fun loadPilots() {
        pilotsListener = db.collection("piloti")
            .addSnapshotListener { documents, e ->
                if (e != null) {
                    Log.w(TAG, "Errore nel recupero dei dati dei piloti", e)
                    return@addSnapshotListener
                }

                documents?.let {
                    for (document in it) {
                        val userId = document.id
                        val lat = document.getDouble("latitude")
                        val lng = document.getDouble("longitude")
                        val name = document.getString("name") ?: "Sconosciuto"
                        val drone = document.getString("drone") ?: "N/D"
                        val availableForChat = document.getBoolean("availableForChat") ?: false

                        if (lat != null && lng != null) {
                            val position = LatLng(lat, lng)
                            val markerOptions = MarkerOptions().position(position).title("$name - $drone")

                            // Cambia il colore del marker in base alla disponibilità per la chat
                            if (availableForChat) {
                                Log.d(TAG, "Stato chat aggiornato per $name: $availableForChat")
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            } else {
                                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            }

                            // Aggiungi la descrizione sotto il titolo del marker
                            markerOptions.snippet("Clicca per aprire la chat con $name")

                            val existingMarker = pilotMarkers[userId]
                            if (existingMarker == null) {
                                // Aggiungi il marker con il suo ID come tag
                                val marker = mMap.addMarker(markerOptions)!!
                                marker.tag = userId  // Tagging con l'ID del pilota
                                pilotMarkers[userId] = marker
                            } else {
                                existingMarker.position = position
                                existingMarker.title = "$name - $drone"
                                existingMarker.setIcon(markerOptions.icon)  // Aggiorna l'icona del marker
                                existingMarker.snippet = markerOptions.snippet // Aggiorna la descrizione
                            }
                        } else {
                            pilotMarkers[userId]?.remove()
                            pilotMarkers.remove(userId)
                        }
                    }
                }
            }
    }




    private fun startFlight(userName: String, droneName: String) {
        if (!::mMap.isInitialized) {
            Toast.makeText(this, "La mappa non è pronta", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userId = auth.currentUser?.uid ?: return@addOnSuccessListener
                val position = hashMapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "name" to userName,
                    "drone" to droneName
                )
                db.collection("piloti").document(userId).set(position)

                val userPosition = LatLng(location.latitude, location.longitude)
                mMap.addMarker(MarkerOptions().position(userPosition).title("$userName - $droneName"))
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15f))

                startLocationUpdates(userId, userName, droneName)
            }
        }
    }

    private fun stopFlight(userName: String) {
        val db = FirebaseFirestore.getInstance()
        val cleanedUserName = userName.trim()  // Rimuove spazi e uniforma il confronto

        Log.d("DronePilotApp", "🔍 Sto cercando il volo per: '$cleanedUserName'")

        db.collection("piloti")
            .whereEqualTo("name", cleanedUserName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("DronePilotApp", "❌ Nessun documento trovato per il nome: $cleanedUserName")
                    Toast.makeText(this, "Nessuna posizione trovata per $cleanedUserName", Toast.LENGTH_SHORT).show()
                } else {
                    for (document in documents) {
                        Log.d("DronePilotApp", "✅ Trovato documento: ${document.id} - ${document.data}")

                        db.collection("piloti").document(document.id).delete()
                            .addOnSuccessListener {
                                Log.d("DronePilotApp", "🗑️ Posizione rimossa con successo per $cleanedUserName")
                                Toast.makeText(this, "Volo terminato con successo", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("DronePilotApp", "❌ Errore nella rimozione della posizione", e)
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DronePilotApp", "❌ Errore nel recupero del documento", e)
            }
    }






    private fun startLocationUpdates(userId: String, userName: String, droneName: String) {
        // Crea una richiesta per gli aggiornamenti della posizione
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // Aggiornamenti ogni 10 secondi
            fastestInterval = 5000 // Aggiornamenti veloci ogni 5 secondi
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Crea un callback per gestire gli aggiornamenti della posizione
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Non è necessario il controllo "if (result != null)"
                for (location in locationResult.locations) {
                    val position = hashMapOf(
                        "latitude" to location.latitude,
                        "longitude" to location.longitude,
                        "name" to userName,
                        "drone" to droneName,
                        "timestamp" to System.currentTimeMillis()
                    )
                    db.collection("piloti").document(userId).set(position)

                    val userPosition = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(userPosition).title("$userName - $droneName"))
                    //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15f))
                }
            }
        }

        // Avvia gli aggiornamenti della posizione
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun onStop() {
        super.onStop()
        // Ferma gli aggiornamenti della posizione quando l'attività è in stop
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pilotsListener?.remove()
    }
}
