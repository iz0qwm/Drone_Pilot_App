package com.kwos.dronepilotapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.Context
import android.os.Build
import android.location.Location

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
import android.widget.ImageView
import android.content.BroadcastReceiver
import android.content.IntentFilter


import androidx.lifecycle.Observer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo


import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
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
import com.google.firebase.firestore.DocumentChange
import java.util.concurrent.TimeUnit
import com.squareup.picasso.Picasso


import com.kwos.dronepilotapp.databinding.ActivityDashboardBinding

class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var chatToggle: Switch
    private lateinit var locationCallback: LocationCallback  // Variabile per il callback della posizione
    private lateinit var binding: ActivityDashboardBinding
    // Dichiarazione del receiver come variabile membro
    private lateinit var messageReceiver: BroadcastReceiver

    private var mapFragment: SupportMapFragment? = null
    private var userName: String? = null  // Ora viene caricato da loadUserName()
    private var droneName: String? = null
    private val TAG = "DronePilotApp"
    private var pilotsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private val pilotMarkers = mutableMapOf<String, Marker>()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //BINDING ??
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        // Imposta il layout
        setContentView(binding.root)

        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        //Esecuzione funzioni automatiche
        CleanupWorker()
        checkForNewMessages()

        // altre variabili
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        // Ora possiamo usare direttamente il binding per accedere agli elementi della UI
        chatToggle = binding.chatToggle  // <-- IMPORTANTE: Prendilo dal binding!
        // Log per confermare
        logDebug(TAG, "✅ chatToggle inizializzato: ${chatToggle != null}")
        //chatToggle = findViewById(R.id.chatToggle)


        //Partenza utente
        loadUserName() // Carica il nome del pilota all'avvio



        // INIZIO BROADCAST RECEIVER
        //Questo serve per ricevere i messaggi provenienti dalla Chat
        // Inizializza il BroadcastReceiver
        messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Controlla se l'intent contiene il messaggio
                val message = intent.getStringExtra("message")
                val title = intent.getStringExtra("title")
                val senderId = intent.getStringExtra("senderId")
                if (message != null && title != null && senderId != null) {
                    showNewMessageInDashboard(title, message, senderId)
                }
            }
        }
        // Registra il receiver per ricevere il broadcast
        val filter = IntentFilter("com.kwos.dronepilotapp.NEW_MESSAGE")
        // Per Android 12 e versioni successive, registriamo dinamicamente il receiver in modo sicuro
        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        //    registerReceiver(messageReceiver, filter, Context.RECEIVER_EXPORTED)
        //} else {
            registerReceiver(messageReceiver, filter, Context.RECEIVER_EXPORTED)
        //}
        // Verifica e richiedi permesso per notifiche su Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        // FINE BROADCAST RECEIVER


        //definizione variabili
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val startFlightButton = findViewById<Button>(R.id.startFlightButton)
        val stopFlightButton = findViewById<Button>(R.id.stopFlightButton)
        val droneField = findViewById<EditText>(R.id.droneField)
        val mapContainer = findViewById<FrameLayout>(R.id.mapContainer)
        //val weatherInfoText = findViewById<TextView>(R.id.weather_info_text)
        val weatherButton: Button = findViewById(R.id.weather_forecast_button)

        //Partenza della mappa
        mapContainer.visibility = View.VISIBLE

        //Aggiorna le posizioni
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    logDebug(TAG, "📍 Nuova posizione ricevuta: ${location.latitude}, ${location.longitude}")
                }
            }
        }

        weatherButton.setOnClickListener {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLat = it.latitude
                    val userLon = it.longitude

                    val intent = Intent(this, WeatherForecastActivity::class.java)
                    intent.putExtra("LATITUDE", userLat) // Inserisci la latitudine reale
                    intent.putExtra("LONGITUDE", userLon) // Inserisci la longitudine reale
                    startActivity(intent)

                    // Ottiene i dati meteo
                    //MeteoManager.getMeteoData(userLat, userLon) { meteo ->
                    //    runOnUiThread {
                    //        if (meteo != null) {
                    //            val weatherText = "Temperatura Min: ${meteo.temperature_min}° - Max: ${meteo.temperature_max}°\n" +
                    //                    "Vento: ${meteo.wind_speedmax} Km/h - ${meteo.wind_speedmean} Km/h - ${meteo.wind_speedmin} Km/h\n " +
                    //                    "Umidità: ${meteo.humidity}%"
                    //            weatherInfoText.text = weatherText
                    //        } else {
                    //            weatherInfoText.text = "Dati meteo non disponibili"
                    //        }
                    //    }
                    //}
                }
            }
        }



        // Controlla l'esecuzione del Logout
        logoutButton.setOnClickListener {
            logout()
        }


        // Tasto Start Flight
        startFlightButton.setOnClickListener {
            droneName = droneField.text.toString()
            if (!userName.isNullOrEmpty() && !droneName.isNullOrEmpty()) {
                startFlight(userName!!, droneName!!)
            } else {
                Toast.makeText(this, "Caricamento nome pilota in corso o nome drone mancante", Toast.LENGTH_SHORT).show()
            }
        }

        //Tasto Stop Flight
        stopFlightButton.setOnClickListener {
            val currentUserName = userName ?: ""  // Evita il nullable
            if (currentUserName.isNotEmpty()) {
                logDebug(TAG, "Tentativo di eliminare il volo per $currentUserName")
                stopFlight(currentUserName)
            } else {
                Toast.makeText(this, "Non posso fermare un volo inesistente", Toast.LENGTH_SHORT).show()
            }
        }

        // Recupera lo stato della disponibilità alla chat da Firestore al login
        auth.currentUser?.uid?.let { userId ->
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        chatToggle.isChecked = document.getBoolean("availableForChat") ?: false
                    }
                }
        }


        // Ora il listener funzionerà sempre
        chatToggle.setOnCheckedChangeListener { _, isChecked ->
            val userId = auth.currentUser?.uid
            if (userId == null) {
                logError(TAG, "❌ chatToggle ERRORE: L'utente non è autenticato, impossibile aggiornare Firestore")
                return@setOnCheckedChangeListener
            }

            db.collection("users").document(userId)
                .update("availableForChat", isChecked)
                .addOnSuccessListener {
                    logDebug(TAG, "✅ chatToggle: Stato chat aggiornato: $isChecked")
                }
                .addOnFailureListener { e ->
                    logError(TAG, "❌ chatToggle: Errore nell'aggiornamento dello stato chat", e)
                }
        }

        showMap()

    }


    /// INIZIO FUNZIONI

    // Mostra messaggi dopo averli recuperati dal Broadcast
    private fun showNewMessageInDashboard(title: String, message: String, senderId: String) {
        // Usa il binding per accedere alle viste
        binding.newMessageContainer.visibility = View.VISIBLE

        // Recupera il nome completo da Firestore
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(senderId).get()
            .addOnSuccessListener { document ->
                val fullName = document.getString("fullName") ?: "Utente sconosciuto"

                // Imposta il testo con il nome e il messaggio insieme
                binding.newMessageText.text = "Nuovo messaggio da $fullName:\n$message"

                // Imposta anche il contentDescription per l'accessibilità
                binding.newMessageContainer.contentDescription = "Nuovo messaggio da $fullName"
            }
            .addOnFailureListener {
                // Se il recupero del nome fallisce, mostra solo il messaggio
                binding.newMessageText.text = "Nuovo messaggio:\n$message"
                binding.newMessageContainer.contentDescription = "Nuovo messaggio da utente sconosciuto"
            }

        // Imposta un'azione di clic sull'icona per aprire la chat con il mittente
        binding.newMessageIcon.setOnClickListener {
            openChatWithPilot(senderId)  // Passa il senderId per aprire la chat con il mittente
        }
    }


    //Carica il nome utente e gli dice Benvenuto
    private fun loadUserName() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userName = document.getString("fullName") // Salva il nome per riutilizzarlo
                        findViewById<TextView>(R.id.welcomeTextView).text = "Benvenuto/a, $userName!"
                    }
                }
                .addOnFailureListener {
                    logError(TAG, "Errore nel recupero del nome dal database", it)
                }
        }
    }

    //Mostra la Mappa
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

    //Fa aprire la Chat con il mittente (receiver) se si clicca sul messaggio ricevuto
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
            listenForChatAvailability()
            logDebug(TAG, "📡 Chat Listener per chat attivato")
        }
    }


    private fun loadPilots() {
        pilotsListener?.remove()  // Rimuove il vecchio listener se già esiste
        pilotsListener = db.collection("piloti")
            .whereEqualTo("inVolo", true)  // Carica solo chi è ancora in volo
            .addSnapshotListener { documents, e ->
                if (e != null) {
                    logWarning(TAG, "Errore nel recupero dei dati dei piloti", e)
                    return@addSnapshotListener
                }

                documents?.documentChanges?.forEach { change ->
                    val userId = change.document.id

                    val lat = change.document.getDouble("latitude")
                    val lng = change.document.getDouble("longitude")
                    val name = change.document.getString("name") ?: "Sconosciuto"
                    val drone = change.document.getString("drone") ?: "N/D"

                    if (lat != null && lng != null) {
                        val position = LatLng(lat, lng)

                        val markerOptions = MarkerOptions().position(position).title("$name - $drone")

                        // Recuperiamo il marker se già esiste
                        val existingMarker = pilotMarkers[userId]
                        if (existingMarker == null) {
                            val marker = mMap.addMarker(markerOptions)!!
                            marker.tag = userId
                            pilotMarkers[userId] = marker
                        } else {
                            existingMarker.position = position
                            existingMarker.title = "$name - $drone"
                            // Aggiorna l'icona per mantenere la coerenza con lo stato della chat
                            existingMarker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        }
                    } else {
                        pilotMarkers[userId]?.remove()
                        pilotMarkers.remove(userId)
                    }
                }
            }
    }

    private fun listenForChatAvailability() {
        logDebug(TAG, "🟢 Chat: Inizializzazione listener per stato chat")

        usersListener = db.collection("users")
            .addSnapshotListener { documents, e ->
                if (e != null) {
                    logWarning(TAG, "❌ Chat: Errore nel recupero dello stato chat", e)
                    return@addSnapshotListener
                }

                if (documents == null || documents.isEmpty) {
                    logWarning(TAG, "⚠️ Chat: Nessun aggiornamento ricevuto da Firestore")
                    return@addSnapshotListener
                }

                logDebug(TAG, "📡 Chat: Aggiornamento ricevuto da Firestore")

                documents?.forEach { doc ->
                    val userId = doc.id
                    val availableForChat = doc.getBoolean("availableForChat") ?: false

                    //Da rimuovere questo log quando gli utenti saranno tanti
                    //RIMUOVERE
                    logDebug(TAG, "🔄 Chat: Stato aggiornato per $userId: $availableForChat")

                    // Recuperiamo il marker e aggiorniamo l'icona e lo snippet
                    val existingMarker = pilotMarkers[userId]
                    if (existingMarker != null) {
                        val markerIcon = if (availableForChat) {
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                        } else {
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        }
                        existingMarker.setIcon(markerIcon)

                        val snippetText = if (availableForChat) {
                            "✅ Disponibile per chat - Clicca per aprire la chat"
                        } else {
                            "❌ Non disponibile per chat"
                        }
                        existingMarker.snippet = snippetText
                        logDebug(TAG, "✅ Chat: Marker aggiornato per $userId")
                    } else {
                        logWarning(TAG, "⚠️ Chat: Marker non trovato per $userId, impossibile aggiornare lo stato chat")
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

                // Imposta "inVolo: true" nella raccolta users
                db.collection("users").document(userId)
                    .update("inVolo", true)
                    .addOnSuccessListener {
                        logDebug(TAG, "🚀 Impostato 'inVolo' su true per $userId")
                    }
                    .addOnFailureListener {
                        logWarning(TAG, "⚠️ Errore nell'impostare 'inVolo' su true", it)
                    }

                val position = hashMapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "name" to userName,
                    "drone" to droneName,
                    "inVolo" to true  // Flag impostato su true
                )
                db.collection("piloti").document(userId).set(position)

                val existingMarker = pilotMarkers[userId]
                existingMarker?.remove()  // Rimuove il marker vecchio prima di crearne uno nuovo

                val userPosition = LatLng(location.latitude, location.longitude)
                val marker = mMap.addMarker(MarkerOptions()
                    .position(userPosition)
                    .title("$userName - $droneName")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))  // Inizialmente rosso
                )!!

                pilotMarkers[userId] = marker  // Salva il marker

                logDebug(TAG, "✅ startFlight: Attivato il volo per: $userId - $userName - $droneName")

                startLocationUpdates(userId, userName, droneName)
            }
        }
    }


    private fun stopFlight(userName: String) {
        pilotsListener?.remove()
        pilotsListener = null
        logDebug(TAG, "🛑 stopFlight: Listener LoadPiloti Firestore rimosso")

        val db = FirebaseFirestore.getInstance()
        val cleanedUserName = userName.trim()  // Rimuove spazi e uniforma il confronto
        val userId = auth.currentUser?.uid ?: return

        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } else {
            logWarning(TAG, "⚠️ stopFlight: locationCallback non è inizializzato, impossibile rimuovere aggiornamenti")
        }

        // Interrompi gli aggiornamenti della posizione
        logDebug(TAG, "🛑 stopFlight: Fermo gli aggiornamenti sulla posizione per: '$cleanedUserName'")
        fusedLocationClient.removeLocationUpdates(locationCallback)

        logDebug(TAG, "🔍 stopFlight: Sto cercando il volo per: '$cleanedUserName'")

        // Imposta "inVolo: false" nella raccolta users
        db.collection("users").document(userId)
            .update("inVolo", false)
            .addOnSuccessListener {
                logDebug(TAG, "🛑 stopFlight: Impostato 'inVolo' su false per $userId")
            }
            .addOnFailureListener {
                logWarning(TAG, "⚠️ stopFlight: Errore nell'impostare 'inVolo' su false", it)
            }
        // Imposta "availabeForChat: false" nella raccolta users
        db.collection("users").document(userId)
            .update("availableForChat", false)
            .addOnSuccessListener {
                logDebug(TAG, "🛑 stopFlight: Impostato 'availableForChat' su false per $userId")
            }
            .addOnFailureListener {
                logWarning(TAG, "⚠️ stopFlight: Errore nell'impostare 'availableForChat' su false", it)
            }

        db.collection("piloti")
            .whereEqualTo("name", cleanedUserName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    logDebug(TAG, "❌ stopFlight: Nessun documento trovato per il nome: $cleanedUserName")
                    Toast.makeText(this, "stopFlight: Nessuna posizione trovata per $cleanedUserName", Toast.LENGTH_SHORT).show()
                } else {
                    for (document in documents) {
                        logDebug(TAG, "✅ stopFlight: Trovato documento: ${document.id} - ${document.data}")

                        db.collection("piloti").document(document.id).delete()
                            .addOnSuccessListener {
                                val userId = document.id
                                val marker = pilotMarkers[userId]
                                logDebug(TAG, "🗑️ stopFlight: Posizione rimossa con successo per $cleanedUserName")
                                Toast.makeText(this, "Volo terminato con successo", Toast.LENGTH_SHORT).show()
                                logDebug(TAG, "🚩 stopFlight: Rimuovendo marker per l'utente: $userId")
                                if (marker != null) {
                                    marker.remove()
                                    pilotMarkers.remove(userId)
                                    logDebug(TAG, "stopFlight: Marker rimosso per $userId")
                                }
                                logDebug(TAG, "🔄 stopFlight: Verifica marker esistenti: ${pilotMarkers.keys}")

                            }
                            .addOnFailureListener { e ->
                                logError(TAG, "❌ stopFlight: Errore nella rimozione della posizione", e)
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                logError(TAG, "❌ stopFlight: Errore nel recupero del documento", e)
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

                    // Controlla se il marker esiste già per l'utente
                    val existingMarker = pilotMarkers[userId]
                    if (existingMarker != null) {
                        // Se il marker esiste già, aggiorna la posizione
                        existingMarker.position = userPosition
                        existingMarker.title = "$userName - $droneName"
                    } else {
                        // Crea un nuovo marker solo se non esiste
                        val marker = mMap.addMarker(MarkerOptions().position(userPosition).title("$userName - $droneName"))
                        marker?.tag = userId // Associa l'ID del pilota al marker
                        pilotMarkers[userId] = marker!!
                    }
                    //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15f))
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

    }

    private fun observeCleanupWorker(workRequest: PeriodicWorkRequest) {
        val workManager = WorkManager.getInstance(this)

        // Osserva il lavoro in corso
        workManager.getWorkInfoByIdLiveData(workRequest.id).observe(this, Observer { workInfo ->
            if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                // Ottieni i dati restituibili dal lavoro
                val userIdsToRemove = workInfo.outputData.getStringArray("userIdsToRemove")?.toList() ?: emptyList()
                if (userIdsToRemove.isNotEmpty()) {
                    // Chiama la funzione per rimuovere i marker
                    removeMarkersForPilots(userIdsToRemove)
                }
            }
        })
    }

    private fun CleanupWorker() {
        val workRequest = PeriodicWorkRequestBuilder<CleanupWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "cleanupWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        // Passa workRequest a observeCleanupWorker
        observeCleanupWorker(workRequest)
    }


    fun removeMarkersForPilots(userIdsToRemove: List<String>) {
        for (userId in userIdsToRemove) {
            pilotMarkers[userId]?.remove()  // Rimuovi il marker dalla mappa
            pilotMarkers.remove(userId)  // Rimuovi l'ID dalla mappa dei piloti
            logDebug(TAG, "Marker rimosso per il pilota $userId.")
        }
    }

    private fun checkForNewMessages() {
        val prefs = getSharedPreferences("ChatPrefs", Context.MODE_PRIVATE)
        val hasNewMessage = prefs.getBoolean("hasNewMessage", false)

        val newMessageContainer = findViewById<View>(R.id.new_message_container)

        if (hasNewMessage) {
            newMessageContainer.visibility = View.VISIBLE
        } else {
            newMessageContainer.visibility = View.VISIBLE
        }
    }

    private fun logout() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        logDebug(TAG, "logout: tasto premuto da $userId.")
        if (userId != null) {
            // Interrompe il volo e rimuove i marker
            stopFlight(userId)
            logDebug(TAG, "logout: stopflight eseguito per $userId.")

            // Rimuove i token FCM dal database
            logDebug(TAG, "logout: FCM Token rimossi per $userId.")
            MyFirebaseMessagingService().removeTokensOnLogout(userId) {
                // Rimuove i listener Firestore attivi (se presenti)
                usersListener?.remove()  // Rimuovi il listener per la chat o altre operazioni di Firestore
                usersListener = null
                pilotsListener?.remove()
                pilotsListener = null
                logDebug(TAG, "logout: Rimuovo i listener per $userId.")
                // Fa il logout
                FirebaseAuth.getInstance().signOut()
                logDebug(TAG, "logout: Utente disconnesso: $userId")
                // Torna alla schermata di login
                logDebug(TAG, "logout: Ricarico la schermata iniziale")
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } else {
            // Effettua il logout da Firebase (deve essere fatto dopo aver completato tutte le operazioni)
            logDebug(TAG, "logout: Utente disconnesso: $userId")
            FirebaseAuth.getInstance().signOut()
            // Torna alla schermata di login
            logDebug(TAG, "logout: Ricarico la schermata iniziale")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }


    override fun onStop() {
        super.onStop()
        // Ferma gli aggiornamenti della posizione quando l'attività è in stop
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onResume() {
        super.onResume()
        checkForNewMessages() // Controlla se ci sono nuovi messaggi
    }

    override fun onDestroy() {
        super.onDestroy()
        usersListener?.remove()
        usersListener = null
        pilotsListener?.remove()
        pilotsListener = null
        // Unregister the receiver when the activity is destroyed
        unregisterReceiver(messageReceiver)
    }




}
