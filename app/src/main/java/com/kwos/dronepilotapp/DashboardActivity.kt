package com.kwos.dronepilotapp


import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.widget.PopupMenu
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
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kwos.dronepilotapp.databinding.ActivityDashboardBinding
import android.util.Log
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.*
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import android.app.AlertDialog
import com.google.firebase.crashlytics.FirebaseCrashlytics
// per il padding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

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
    private lateinit var pilotNearAlert: TextView
    private lateinit var lowerLimitTextView: TextView


    private var mapFragment: SupportMapFragment? = null
    private var userName: String? = null  // Ora viene caricato da loadUserName()
    private var droneName: String? = null
    private val TAG = "DronePilotApp"
    private var pilotsListener: ListenerRegistration? = null
    private var usersListener: ListenerRegistration? = null
    private val pilotMarkers = mutableMapOf<String, Marker>()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            loadPilots() // Ricarica tutti i piloti
            handler.postDelayed(this, 60000) // Ripeti ogni 60 secondi
        }
    }



    //Gestione ricerca piloti
    private var pilotsLoaded = false
    private var retryAttempts = 0
    private val maxRetryAttempts = 10



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Abilita l'invio dei crash su Firebase console
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        //BINDING ??
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        // Imposta il layout
        setContentView(binding.root)

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(
                top = systemBars.top, // Adatta per la status bar
                bottom = systemBars.bottom // Adatta per la navigation bar
            )

            WindowInsetsCompat.CONSUMED
        }
        // fine padding


        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()



// Aggiungi il callback per gestire il tasto indietro
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Usa 'this@DashboardActivity' per ottenere il contesto
                val builder = AlertDialog.Builder(this@DashboardActivity)
                builder.setMessage("Sei sicuro di voler uscire?")
                    .setCancelable(false)
                    .setPositiveButton("Sì") { _, _ ->
                        logout()
                        finish() // Chiude l'attività invece di chiamare super.onBackPressed()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss() // Se l'utente annulla, non succede nulla
                    }
                val alert = builder.create()
                alert.show()
            }
        })


        //Esecuzione funzioni automatiche
        // Controllo la presenza di nuovi messaggi
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
        val menuButton: ImageButton = findViewById(R.id.menuButton)
        //val lowerLimitTextView: TextView = findViewById(R.id.lowerLimitTextView)


        //val pilotNearAlert = findViewById<TextView>(R.id.pilotNearAlert)


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

        menuButton.setOnClickListener { view ->
            showPopupMenu(view)
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

        // Mostra la mappa
        showMap()

        //vede se si può volare di d-flight JASON
        // Verifica se la TextView esiste nel layout
        findViewById<TextView>(R.id.lowerLimitTextView)?.text = "In attesa...."
        fetchFlightLimitWithLocation()

    }


    /// INIZIO FUNZIONI

    // Mostra il menu in Popup
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_options, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_piloti_online -> {
                    startActivity(Intent(this, PilotiOnlineActivity::class.java))
                    true
                }
                R.id.menu_impostazioni -> {
                    startActivity(Intent(this, ImpostazioniActivity::class.java))
                    true
                }
                R.id.menu_informazioni -> {
                    startActivity(Intent(this, InformazioniActivity::class.java))
                    true
                }
                R.id.menu_group_chat -> {
                    startActivity(Intent(this, GroupChatActivity::class.java))
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

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
        // Imposta un'azione di clic sul messaggio per aprire la chat con il mittente
        binding.newMessageText.setOnClickListener {
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
        logDebug(TAG, "DashboardActivity: openChatWithPilot")
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("receiverId", userId)  // Assicurati di usare la chiave giusta
        val senderId = FirebaseAuth.getInstance().currentUser?.uid
        intent.putExtra("senderId", senderId)
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
        logDebug(TAG, "🔄 loadPilots: Caricamento piloti in corso...")

        pilotsListener?.remove()  // Rimuove il vecchio listener se già esiste
        pilotsListener = db.collection("users")
            .whereEqualTo("inVolo", true)  // Cerca solo gli utenti che hanno inVolo = true
            .addSnapshotListener { userDocs, e ->
                if (e != null) {
                    logWarning(TAG, "loadPilots: Errore nel recupero degli utenti in volo", e)
                    return@addSnapshotListener
                }

                if (userDocs == null || userDocs.isEmpty) {
                    logWarning(TAG, "⚠️ loadPilots: Nessun pilota in volo trovato...")
                    return@addSnapshotListener
                }

                logDebug(TAG, "📡 loadPilots: Piloti in volo trovati: ${userDocs.size()}")

                userDocs.forEach { userDoc ->
                    val userId = userDoc.id

                    // Ora cerchiamo le coordinate nella collezione "piloti"
                    db.collection("piloti").document(userId)
                        .get()
                        .addOnSuccessListener { pilotDoc ->
                            if (pilotDoc.exists()) {
                                val lat = pilotDoc.getDouble("latitude")
                                val lng = pilotDoc.getDouble("longitude")
                                val name = pilotDoc.getString("name") ?: "Sconosciuto"
                                val drone = pilotDoc.getString("drone") ?: "N/D"

                                if (lat != null && lng != null) {
                                    val position = LatLng(lat, lng)

                                    val markerOptions = MarkerOptions().position(position).title("$name - $drone")
                                    logDebug(TAG, "🔄 loadPilots: Aggiungendo/aggiornando marker per $userId")

                                    val availableForChat = userDoc.getBoolean("availableForChat") ?: false // Aggiungi questa riga per recuperare la disponibilità per la chat

                                    //Controllo lo stato della chat
                                    val markerIcon = if (availableForChat) {
                                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE) // Blu se disponibile per la chat
                                    } else {
                                        BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED) // Rosso se non disponibile
                                    }


                                    // Aggiungi un marker o aggiorna il marker esistente
                                    val existingMarker = pilotMarkers[userId]
                                    if (existingMarker == null) {
                                        val marker = mMap.addMarker(markerOptions)!!
                                        marker.tag = userId
                                        marker.setIcon(markerIcon) // Imposta il colore corretto
                                        pilotMarkers[userId] = marker
                                        logDebug(TAG, "✅ Marker aggiunto per $userId")
                                    } else {
                                        existingMarker.position = position
                                        //existingMarker.title = "$name - $drone"
                                        existingMarker.setIcon(markerIcon) // Imposta il colore corretto
                                        logDebug(TAG, "✅ loadPilots: Marker esistente aggiornato per $userId")
                                    }
                                } else {
                                    pilotMarkers[userId]?.remove()
                                    pilotMarkers.remove(userId)
                                    logDebug(TAG, "❌ loadPilots: Marker rimosso per $userId")
                                }
                            } else {
                                logWarning(TAG, "⚠️ loadPilots: Nessun dato trovato in 'piloti' per $userId verrà rimesso in inVolo:false dal server")
                            }
                        }
                        .addOnFailureListener { err ->
                            logWarning(TAG, "❌ loadPilots: Errore nel recupero delle coordinate per $userId", err)
                        }
                    // Aggiungi il log per confermare che i piloti sono stati caricati
                    logDebug(TAG, "✅ loadPilots: Piloti caricati, impostazione di pilotsLoaded a true")
                    pilotsLoaded = true
                }
            }
    }


    private fun listenForChatAvailability() {
        logDebug(TAG, "🟢 ChatAvail: Inizializzazione listener per stato chat")

        // Esegui il check per assicurarti che i piloti siano stati caricati
        if (!pilotsLoaded) {
            logWarning(TAG, "⚠️ ChatAvail: Piloti non ancora caricati, attesa...")

            if (retryAttempts >= maxRetryAttempts) {
                logWarning(TAG, "❌ ChatAvail: Troppi tentativi, piloti non caricati.")
                return  // Esce se il numero massimo di tentativi è stato raggiunto
            }

            // Incrementa il numero di tentativi
            retryAttempts++

            // Esegui un backoff esponenziale (doppia attesa per ogni tentativo fallito)
            val delayTime = (500 * Math.pow(2.0, retryAttempts.toDouble())).toLong()

            // Riprovare dopo un breve intervallo
            Handler(Looper.getMainLooper()).postDelayed({
                listenForChatAvailability()  // Riprova dopo l'intervallo
            }, delayTime)
            return
        }

        // Se i piloti sono caricati, resetta il contatore dei tentativi
        retryAttempts = 0

        // Avvia il listener per lo stato della chat
        usersListener = db.collection("users")
            .addSnapshotListener { documents, e ->
                if (e != null) {
                    logWarning(TAG, "❌ ChatAvail: Errore nel recupero dello stato chat", e)
                    return@addSnapshotListener
                }

                if (documents == null || documents.isEmpty) {
                    logWarning(TAG, "⚠️ ChatAvail: Nessun aggiornamento ricevuto da Firestore")
                    return@addSnapshotListener
                }

                logDebug(TAG, "📡 ChatAvail: Aggiornamento ricevuto da Firestore")

                documents?.forEach { doc ->
                    val userId = doc.id
                    val availableForChat = doc.getBoolean("availableForChat") ?: false
                    val inVolo = doc.getBoolean("inVolo") ?: false // Recupera lo stato di volo

                    // Evita di processare i marker di chi non è in volo
                    if (!inVolo) {
                        logDebug(TAG, "🚫 Chat: $userId non è in volo, marker ignorato")
                        return@forEach
                    }

                    // Da rimuovere questo log quando gli utenti saranno tanti
                    logDebug(TAG, "🔄 ChatAvail: Stato aggiornato per $userId: $availableForChat")

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
                        logDebug(TAG, "✅ ChatAvail: Marker aggiornato per $userId")
                    } else {
                        logWarning(TAG, "⚠️ ChatAvail: Questo $userId non ha un marker attivo, verrà resettato lo stato dal server")
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
                        logDebug(TAG, "🚀 startFlight: Impostato 'inVolo' su true per $userId")
                    }
                    .addOnFailureListener {
                        logWarning(TAG, "⚠️ startFlight: Errore nell'impostare 'inVolo' su true", it)
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

                // Controlliamo se ha lo stato di availableForChat
                val userRef = FirebaseFirestore.getInstance().collection("pilots").document(userId)

                userRef.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        val inVolo = document.getBoolean("inVolo") ?: false
                        val availableForChat = document.getBoolean("availableForChat") ?: false

                        if (inVolo) {
                            aggiornaMarker(userId, availableForChat)
                            logDebug(TAG, "🚀 startFlight: Aggiorno marker per $userId")
                        } else {
                            logDebug(TAG, "⚠️ startFlight: Il pilota $userId non era in volo")
                        }
                    }
                }.addOnFailureListener { e ->
                    logError(TAG, "❌ startFlight: Errore nel recupero dello stato di volo: ${e.message}")
                }


            }
        }.addOnFailureListener {
            logWarning(TAG, "⚠️ startFlight: Errore nel recupero della posizione", it)
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
        logDebug(TAG, "LocationUpdates: START Aggiornamento posizioni per $userId con drone $droneName")
        // Crea una richiesta per gli aggiornamenti della posizione
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 50000)
            .setMinUpdateIntervalMillis(120000) // Intervallo minimo di aggiornamento
            .build()


        // Crea un callback per gestire gli aggiornamenti della posizione
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                logDebug(TAG, "📍 LocationUpdates: Nuova posizione ricevuta (${locationResult.locations.size} per $userId)")
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
                        logDebug(TAG, "LocationUpdates: Il marker per $userId esiste già, lo aggiorno")
                        // Se il marker esiste già, aggiorna la posizione
                        existingMarker.position = userPosition
                        //existingMarker.title = "$userName - $droneName"
                    } else {
                        // Crea un nuovo marker solo se non esiste
                        logError(TAG, "LocationUpdates: QUI NON DEVE MAI ENTRARE vuol dire che non ho trovato il marker di $userId")
                        //val marker = mMap.addMarker(MarkerOptions().position(userPosition).title("$userName - $droneName"))
                        //marker?.tag = userId // Associa l'ID del pilota al marker
                        //pilotMarkers[userId] = marker!!
                    }
                    // Recupera la lista di piloti nelle vicinanze
                    checkNearbyPilots(location.latitude, location.longitude, userId)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 8f))
                }
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

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

    private fun aggiornaMarker(userId: String, availableForChat: Boolean) {
        logDebug(TAG, "✅ aggiornaMarker: sono in aggiornaMarker")
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
            logDebug(TAG, "✅ Marker aggiornato per $userId")
        } else {
            logWarning(TAG, "⚠️ Nessun marker trovato per $userId")
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
            //logDebug(TAG, "logout: Utente disconnesso: $userId")
            //FirebaseAuth.getInstance().signOut()
            // Torna alla schermata di login
            //logDebug(TAG, "logout: Ricarico la schermata iniziale")
            //startActivity(Intent(this, MainActivity::class.java))
            //finish()
        }
    }


    fun leggiLogin(): String? {
        val prefs = getSharedPreferences("DronePilotAppPrefs", MODE_PRIVATE)
        return prefs.getString("user_email", null)
    }

    override fun onStart() {
        super.onStart()
        val user = FirebaseAuth.getInstance().currentUser
        leggiLogin()

        auth.currentUser?.getIdToken(true)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                logDebug(TAG, "🔄 onStart Dashboard: Token aggiornato, riprovo accesso a Firestore")
                recuperaDatiPilota()
            } else {
                logError(TAG, "❌ onStart Dashboard: Errore aggiornamento token: ${task.exception?.message}")
            }
        }
        if (user == null) {
            logError(TAG, "⚠️ onStart Dashboard: Utente disconnesso al resume dell'app")
            return
        }
        logDebug(TAG, "✅ onStart Dashboard: Utente loggato: ${user.email}")
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("pilots").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val inVolo = document.getBoolean("inVolo") ?: false
                val availableForChat = document.getBoolean("availableForChat") ?: false

                if (inVolo) {
                    aggiornaMarker(userId, availableForChat)
                    logDebug(TAG, "🚀 onStart Dashboard: Ripristinato stato di volo per $userId")
                } else {
                    logDebug(TAG, "⚠️ onStart Dashboard: Il pilota $userId non era in volo")
                }
            }
        }.addOnFailureListener { e ->
            logError(TAG, "❌ onStart Dashboard: Errore nel recupero dello stato di volo: ${e.message}")
        }
    }

    private fun recuperaDatiPilota() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val userRef = FirebaseFirestore.getInstance().collection("pilots").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val inVolo = document.getBoolean("inVolo") ?: false
                val availableForChat = document.getBoolean("availableForChat") ?: false

                if (inVolo) {
                    aggiornaMarker(userId, availableForChat)
                    logDebug(TAG, "🚀 recuperaDatiPilota: Ripristinato stato di volo per $userId")
                } else {
                    logDebug(TAG, "⚠️ recuperaDatiPilota: Il pilota $userId non era in volo")
                }
            }
        }.addOnFailureListener { e ->
            logError(TAG, "❌ recuperaDatiPilota: Errore nel recupero dello stato di volo: ${e.message}")
        }
    }

    // Funzione per calcolare la distanza tra due coordinate geografiche (Haversine)
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // raggio della Terra in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c // distanza in km
    }

    // Funzione per controllare la posizione dei piloti nelle vicinanze
    fun checkNearbyPilots(userLat: Double, userLon: Double, currentUserName: String) {
        logDebug(TAG, "checkNearbyPilots: Controlliamo se vi sono piloti nelle vicinanze")

        db.collection("piloti")
            .get()
            .addOnSuccessListener { result ->
                var nearbyPilotsFound = false

                for (document in result) {
                    // Escludi il pilota corrente
                    if (document.id == currentUserName) continue

                    val pilotLat = document.getDouble("latitude") ?: 0.0
                    val pilotLon = document.getDouble("longitude") ?: 0.0

                    // Calcola la distanza tra l'utente e il pilota
                    val distance = calculateDistance(userLat, userLon, pilotLat, pilotLon)
                    if (distance < 1) {  // Se la distanza è inferiore a 1 km
                        nearbyPilotsFound = true
                        break
                    }
                }

                // Se ci sono piloti nelle vicinanze, mostra il messaggio
                if (nearbyPilotsFound) {
                    showNearbyPilotAlert()
                }
            }
    }


    // Funzione per mostrare l'alert del pilota nelle vicinanze
    fun showNearbyPilotAlert() {
        logDebug(TAG, "showNearbyPilotAlert: Pilota nelle vicinanze")
        findViewById<TextView>(R.id.pilotNearAlert)?.apply {
            text = "Attenzione! Piloti nelle vicinanze!"
            visibility = View.VISIBLE
            setTextColor(getColor(R.color.red))
        }
    }

    private fun fetchFlightLimitWithLocation() {
        // Controllo dei permessi per la posizione
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        // Recupera la posizione attuale
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    // Chiama direttamente fetchFlightLimit con la posizione
                    val latitude = it.latitude
                    val longitude = it.longitude
                    fetchFlightLimit(latitude, longitude) { lowerLimit ->
                        // Aggiorna l'interfaccia con il valore del lowerLimit
                        runOnUiThread {
                            findViewById<TextView>(R.id.lowerLimitTextView)?.apply {
                                text = "Open Category fino a: $lowerLimit m\n" +
                                        "controlla D-Flight prima di accendere il drone"
                                visibility = View.VISIBLE
                                setTextColor(getColor(R.color.red))
                            }
                        }
                    }
                } ?: logError(TAG, "fetchCurrentLocation: Errore: Nessuna posizione disponibile")
            }
            .addOnFailureListener { e ->
                logError(TAG, "fetchCurrentLocation: Errore nel recupero della posizione: ${e.message}")
            }
    }

    fun fetchFlightLimit(latitude: Double, longitude: Double, callback: (String) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "UnknownUser"

        // Crea un oggetto JSON per i dati da inviare
        val jsonData = JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("userId", userId)
        }

        // Esegui la richiesta HTTP in un thread di background
        GlobalScope.launch(Dispatchers.IO) {
            val result = sendPostRequest(jsonData)

            withContext(Dispatchers.Main) {
                val lowerLimit = try {
                    val jsonResponse = JSONObject(result)
                    jsonResponse.optString("lowerLimit", "Errore")
                } catch (e: Exception) {
                    logError(TAG, "fetchFlightLimit: Errore nel parsing della risposta: ${e.message}")
                    "Errore nel parsing della risposta"
                }

                findViewById<TextView>(R.id.lowerLimitTextView)?.apply {
                    text = lowerLimit
                    visibility = View.VISIBLE
                    setTextColor(getColor(R.color.red))
                }

                callback(lowerLimit)
            }
        }
    }

    // Funzione per inviare la richiesta HTTP POST
    private fun sendPostRequest(jsonData: JSONObject): String {
        val url = URL("https://us-central1-tutto-sui-droni-community.cloudfunctions.net/getFlightLimit")
        var result = "Errore nel server"

        try {
            val urlConnection = url.openConnection() as HttpURLConnection
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/json")
            urlConnection.doOutput = true

            // Scrivi i dati JSON nel corpo della richiesta
            urlConnection.outputStream.write(jsonData.toString().toByteArray(Charsets.UTF_8))
            urlConnection.outputStream.flush()

            // Leggi la risposta
            val inputStream = urlConnection.inputStream
            val reader = inputStream.bufferedReader()
            result = reader.readText()

            // Chiudi la connessione
            urlConnection.disconnect()
        } catch (e: Exception) {
            Log.e("sendPostRequest", "Errore nella richiesta HTTP: ${e.message}")
        }

        return result
    }

    override fun onStop() {
        super.onStop()
        // Ferma gli aggiornamenti della posizione quando l'attività è in stop
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(messageReceiver)
        // Ferma il refresh dell'handler di loadpilots quando l'app è in pausa
        handler.removeCallbacks(refreshRunnable)
    }


    override fun onResume() {
        super.onResume()
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

        checkForNewMessages() // Controlla se ci sono nuovi messaggi
        val userId = auth.currentUser?.uid
        if (userId != null && userName != null && droneName != null) {
            logDebug(TAG, "onResume: faccio partire startLocationUpdates per $userId - $userName - $droneName")

            startLocationUpdates(userId, userName!!, droneName!!)
        }
        // Ricarica la lista dei piloti quando l'app torna in primo piano, utilizzando l'handler
        handler.post(refreshRunnable) // Avvia il refresh quando l'app torna attiva

    }

    override fun onDestroy() {
        super.onDestroy()
        logDebug(TAG, "⚠️ onDestroy: Attività distrutta")
        usersListener?.remove()
        usersListener = null
        pilotsListener?.remove()
        pilotsListener = null
        // Unregister the receiver when the activity is destroyed
        //unregisterReceiver(messageReceiver)
        logout()
    }


}
