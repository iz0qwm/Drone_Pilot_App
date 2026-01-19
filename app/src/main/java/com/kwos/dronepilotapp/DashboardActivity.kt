package com.kwos.dronepilotapp


import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
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
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color
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
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.*
import androidx.activity.OnBackPressedCallback
import android.app.AlertDialog
import android.content.res.Resources
import android.widget.ImageView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.widget.PopupMenu
import android.widget.ScrollView
import androidx.annotation.RequiresPermission
// per il padding
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

// Per Drone ID
import com.kwos.dronepilotapp.droneid.OpenDroneIdDataManager
import com.kwos.dronepilotapp.data.AircraftObject

// Per le icone a Marker
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.net.Uri

// Per l'assistente
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Spinner
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabsIntent
import androidx.cardview.widget.CardView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Locale

// Per layer meteo
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.Calendar


// Per i layers su google maps

// Per ascoltare il listener dei nuovi messaggi sulla GroupChat
import com.google.firebase.database.ChildEventListener

// Per import spinner lista droni
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kwos.dronepilotapp.QuizHomeActivity
import java.text.SimpleDateFormat

// Per tema HUD CRPC
import android.view.ContextThemeWrapper
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

// Per il Wear OS
import com.kwos.dronepilotapp.wear.WearMessageSender

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
    private lateinit var droneIdDataManager: OpenDroneIdDataManager

    // Launcher per Activity e passare i droni
    private lateinit var settingsLauncher: ActivityResultLauncher<Intent>

    // Mapper per riconoscere Costruttore e modello di drone
    private lateinit var prefixMap: Map<String, String>
    private lateinit var modelMap: Map<String, String>

    // Assistente vocale
    private lateinit var assistant: FlightZoneAssistant
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechIntent: Intent

    //Layers su mappa
    private lateinit var flightZoneLayer: FlightZoneLayer
    private var zonesVisible = false
    private lateinit var weatherLayerManager: WeatherLayerManager
    private var rainTimestamp: String? = null
    private var aircraftLayer: AircraftLayer? = null
    private var aircraftLayerVisible = false

    // Ricevitori DroneID
    private var bluetoothReceiver: BluetoothReceiver? = null
    private var wifiAwareReceiver: WifiAwareReceiver? = null
    private var wifiBeaconReceiver: WifiBeaconReceiver? = null


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

    // variabile per messageReceiver
    private var isMessageReceiverRegistered = false

    // Markers per Drone ID
    private var isDroneReceiverRegistered = false
    private val droneMarkers = mutableMapOf<String, Marker>()
    private val droneTrajectories = mutableMapOf<String, Polyline>()
    private val lastUpdateMap = mutableMapOf<String, Long>()


    //Gestione ricerca piloti
    private var pilotsLoaded = false
    private var retryAttempts = 0
    private val maxRetryAttempts = 10

    // Full screen della mappa
    private var isFullscreen = false

    // Per ricerca luogo
    private var searchMarker: Marker? = null

    // Text to speach
    private var tts: TextToSpeech? = null

    // Lista droni
    private lateinit var droneSpinner: Spinner
    private var droneList = listOf<String>()
    private var selectedDrone: String? = null

    // Per la Checklist Pre volo
    private val CHECKLIST_REQUEST_CODE = 1234
    private lateinit var checklistLauncher: ActivityResultLauncher<Intent>

    // Pressione OK per scadenza documenti
    private var alertAlreadyShownThisSession = false

    // --- CRPC: stato marker + ring ---
    private val crpcMarkers = mutableMapOf<String, Marker>()            // receiverId -> Marker
    private val crpcLastLatLng = mutableMapOf<String, LatLng>()         // per centrare ring
    private var crpcRingCircle: com.google.android.gms.maps.model.Circle? = null
    private var crpcRingAnimator: ValueAnimator? = null
    private var lastCrpcAlertTs: Long = 0L
    private var crpcReceiversListener: ListenerRegistration? = null
    private var crpcAlertsListener: ListenerRegistration? = null
    // Blink marker CRPC
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val crpcBlinkRunnables = mutableMapOf<String, Runnable>()
    private val crpcBlinkOn = mutableMapOf<String, Boolean>()  // stato toggle per ogni receiverId
    // Mini HUD CRPC
    private lateinit var crpcHud: View
    private lateinit var hudBandFreq: TextView
    private lateinit var hudLabel: TextView
    private lateinit var hudRssi: TextView
    private lateinit var hudDistance: TextView
    // --- CRPC DF marker (drone sulla punta del settore) ---
    private var crpcDfMarker: Marker? = null
    private var crpcDfIcon: BitmapDescriptor? = null


    // Ultimo alert noto per ciascun receiver
    data class CrpcAlertInfo(
        val band: String?,
        val freqMhz: Double?,
        val label: String?,
        val rssiDbm: Double?,
        val radiusM: Double?,
        val tsIso: String?,
        val bearingDeg: Double?,         // NEW
        val dfConfidence: Double?        // NEW (0..1)
    )

    private val lastAlertByReceiverId = mutableMapOf<String, CrpcAlertInfo>()
    private var hudAutoHideRunnable: Runnable? = null


    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Abilita l'invio dei crash su Firebase console
        //FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        FirebaseCrashlytics.getInstance().setCustomKey("app_version", BuildConfig.VERSION_NAME)
        FirebaseCrashlytics.getInstance().checkForUnsentReports()
        FirebaseCrashlytics.getInstance().sendUnsentReports()


        //BINDING ??
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        // Imposta il layout
        setContentView(binding.root)

        // INIZIO PADDING
        // EDGE-TO-EDGE
        // Modalità edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false) // Abilita modalità edge-to-edge

        // Imposta se il contenuto della status bar deve essere scuro (true) o chiaro (false)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false // o false, dipende dal tema

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        // FINE EDGE-TO-EDGE

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        // GESTIONE INSETS
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // SOLO paddingBottom per evitare che l'ultima parte vada sotto la navigation bar
            view.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        // fine padding
        // Nasconde la Action Bar
        supportActionBar?.hide()
        // FINE PADDING

        //
        // Gestione HUD Ricevitore CRPC
        //
        val root = findViewById<ViewGroup>(android.R.id.content) // contenitore della Activity

        // 1) Crea un inflater con tema Material SOLO per l’HUD
        val compatCtx = ContextThemeWrapper(this, R.style.AppCompatSafeDialogOverlay)
        val themedInflater = LayoutInflater.from(compatCtx)

        // 2) Infla l'HUD SENZA attach, così ottieni la view e poi la aggiungi
        val hudView = themedInflater.inflate(R.layout.hud_crpc, root, false)
        root.addView(hudView)

        // 3) Usa le view dall'HUD
        val hudRoot = hudView.findViewById<View>(R.id.crpcHud)
        val btnClose = hudView.findViewById<ImageButton>(R.id.crpcHudClose)
        btnClose.setOnClickListener { hideCrpcHud() }


        //
        // Gestione bottone di ricerca indirizzo
        //
        val openSearchPanelButton = findViewById<Button>(R.id.openSearchPanelButton)
        val addressSearchPanel = findViewById<LinearLayout>(R.id.addressSearchPanel)
        val addressInput = findViewById<EditText>(R.id.addressInput)
        val btnGeocode = findViewById<Button>(R.id.btnGeocode)
        val btnLocate = findViewById<Button>(R.id.btnLocate)

        // Mostra/nasconde il pannello
        openSearchPanelButton.setOnClickListener {
            if (addressSearchPanel.visibility == View.GONE) {
                addressSearchPanel.visibility = View.VISIBLE
                addressInput.requestFocus()
            } else {
                addressSearchPanel.visibility = View.GONE
            }
        }

        // AR View
        val arViewButton: Button = findViewById(R.id.ar_view_button)
        arViewButton.setOnClickListener {
            val intent = Intent(this, ARSpotViewerActivity::class.java)
            startActivity(intent)
        }

        // Geocoding
        btnGeocode.setOnClickListener {
            val locationName = addressInput.text.toString()
            if (locationName.isNotBlank()) {
                val geocoder = Geocoder(this, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(locationName, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val location = LatLng(addr.latitude, addr.longitude)
                    searchMarker?.remove()
                    searchMarker = mMap.addMarker(
                        MarkerOptions().position(location).title("Zona cercata")
                    )
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 8f))
                    fetchFlightLimit(location.latitude, location.longitude) { lowerLimit ->
                        Toast.makeText(this, "Open Category fino a $lowerLimit m", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, "Indirizzo non trovato", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Torna alla mia posizione
        btnLocate.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        val userLatLng = LatLng(it.latitude, it.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 8f))
                        addressSearchPanel.visibility = View.GONE

                        // ❌ Rimuovi marker di ricerca, se presente
                        searchMarker?.remove()
                        searchMarker = null
                        fetchFlightLimit(location.latitude, location.longitude) { lowerLimit ->
                            Toast.makeText(this, "Open Category fino a $lowerLimit m", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            }
        }
        //

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

        //Partenza listenere per Notifiche messaggi in Group Chat
        listenForGroupChatNotifications()

        // launcher per Impostazioni
        settingsLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadDronesForSpinner()  // Funzione da richiamare per ricaricare lo Spinner
            }
        }

        // launcher per CheckList
        checklistLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                val safeUserName = userName ?: "Pilota Anonimo"
                val safeDroneName = selectedDrone ?: "Drone Sconosciuto"
                if (!safeUserName.isNullOrBlank() && !safeDroneName.isNullOrBlank()) {
                    startFlight(safeUserName, safeDroneName)
                }
            }
        }

        // INIZIO BROADCAST RECEIVER
        messageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val message = intent.getStringExtra("message")
                val title = intent.getStringExtra("title")
                val senderId = intent.getStringExtra("senderId")
                if (message != null && title != null && senderId != null) {
                    Log.d("DronePilotApp", "Broadcast MessageReceiver: Messaggio ricevuto: $title - $message da $senderId")
                    showNewMessageInDashboard(title, message, senderId)
                }
            }
        }

        // Permessi per notifiche su Android 13+
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
        //val droneField = findViewById<EditText>(R.id.droneField)
        val mapContainer = findViewById<FrameLayout>(R.id.mapContainer)
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        //val weatherInfoText = findViewById<TextView>(R.id.weather_info_text)
        val weatherButton: Button = findViewById(R.id.weather_forecast_button)
        val menuButton: ImageButton = findViewById(R.id.menuButton)
        val spotButton: Button = findViewById(R.id.takeoff_spots_button)
        //val lowerLimitTextView: TextView = findViewById(R.id.lowerLimitTextView)
        //val pilotNearAlert = findViewById<TextView>(R.id.pilotNearAlert)
        val onlineUsersText = findViewById<TextView>(R.id.onlineUsersText)
        val chatUsersText = findViewById<TextView>(R.id.chatUsersText)
        val driLed = findViewById<ImageView>(R.id.driLed)
        val dronezineButton = findViewById<ImageButton>(R.id.dronezineButton)
        //val dflightButton = findViewById<ImageButton>(R.id.dflightButton)
        val droneSkyCheckButton = findViewById<ImageButton>(R.id.dflightButton)
        val voiceBtn = findViewById<Button>(R.id.voiceZoneButton)
        val layersButton = findViewById<ImageButton>(R.id.layersButton)
        val mapCard = findViewById<MaterialCardView>(R.id.mapCard)
        crpcHud = findViewById(R.id.crpcHud)
        hudBandFreq = findViewById(R.id.crpcHudBandFreq)
        hudLabel = findViewById(R.id.crpcHudLabel)
        hudRssi = findViewById(R.id.crpcHudRssi)
        hudDistance = findViewById(R.id.crpcHudDistance)
        findViewById<ImageButton>(R.id.crpcHudClose).setOnClickListener { hideCrpcHud() }


        // Mettiamo stopFlight a 0
        stopFlightButton.isEnabled = false // all'inizio
        // sistemiamo droneName
        droneName = intent.getStringExtra("droneName")
            ?: getSharedPreferences("prefs", MODE_PRIVATE).getString("ultimoDrone", null)
                    ?: "Drone Sconosciuto"


        // layer trasparente davanti alla mappa per intercettare il tocco di due dita
        // e non passarlo alla scroll view
        val touchInterceptor = findViewById<TransparentTouchView>(R.id.touchInterceptor)

        touchInterceptor.onTouchInterceptListener = { disallow ->
            scrollView.requestDisallowInterceptTouchEvent(disallow)
        }

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
            val latLng = searchMarker?.position
            if (latLng != null) {
                val intent = Intent(this, WeatherForecastActivity::class.java)
                intent.putExtra("LATITUDE", latLng.latitude)
                intent.putExtra("LONGITUDE", latLng.longitude)
                startActivity(intent)
            } else {
                // Fallback alla posizione attuale
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        val intent = Intent(this, WeatherForecastActivity::class.java)
                        intent.putExtra("LATITUDE", it.latitude)
                        intent.putExtra("LONGITUDE", it.longitude)
                        startActivity(intent)
                    } ?: run {
                        Toast.makeText(this, "Posizione non disponibile", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        spotButton.setOnClickListener {
            val intent = Intent(this, TakeoffSpotsActivity::class.java)
            startActivity(intent)
        }

        // Controlla l'esecuzione del Logout
        logoutButton.setOnClickListener {
            // Chiudo il volo esistente se faccio logout
            val currentUserName = userName
            if (!currentUserName.isNullOrEmpty()) {
                logDebug(TAG, "Tentativo di eliminare il volo per $currentUserName")
                stopFlight(currentUserName)
            } else {
                Toast.makeText(this, "Non posso fermare un volo inesistente", Toast.LENGTH_SHORT).show()
            }
            // faccio logout
            logout()
        }


        // Controllo presenza Coupon
        val promoPrefs = getSharedPreferences("promo_prefs", MODE_PRIVATE)
        val showCoupon = promoPrefs.getBoolean("showDronezineCoupon", true)

        if (showCoupon) {
            Firebase.firestore.collection("coupons").document("dronezine").get()
                .addOnSuccessListener { document ->
                    val active = document.getBoolean("active") ?: false
                    if (active) {
                        startActivity(Intent(this, CouponActivity::class.java))
                    }
                }
        }


        // Spinner per recupero drone
        droneSpinner = findViewById(R.id.droneSpinner)
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(this, "Utente non autenticato", Toast.LENGTH_SHORT).show()
            return
        }

        //val droneSpinner = findViewById<Spinner>(R.id.droneSpinner)

        FirebaseFirestore.getInstance()
            .collection("pilotProfiles").document(uid)
            .collection("drones")
            .get()
            .addOnSuccessListener { result ->
                val droneList = if (result.isEmpty) {
                    listOf("Nessun drone disponibile")
                } else {
                    result.map { it.getString("name") ?: "Senza nome" }
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, droneList)
                droneSpinner.adapter = adapter

                // 🔹 Recupera ultimo drone usato
                val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
                val ultimoDrone = prefs.getString("ultimoDrone", null)
                val posizioneDefault = droneList.indexOfFirst { it == ultimoDrone }
                if (posizioneDefault >= 0) {
                    droneSpinner.setSelection(posizioneDefault)
                    selectedDrone = droneList[posizioneDefault]
                }

                droneSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        selectedDrone = if (droneList[position] == "Nessun drone disponibile") null else droneList[position]
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {
                        selectedDrone = null
                    }
                }
            }




        // Tasto Start Flight
        // 🔹 Salva l'ultimo drone selezionato
        FirebaseFirestore.getInstance()
            .collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                userName = document.getString("fullName") ?: "Pilota Anonimo"
                val avatarUrl = document.getString("avatarUrl") ?: ""

                stopFlightButton.isEnabled = true

                startFlightButton.setOnClickListener {
                    val safeUserName = userName ?: "Pilota Anonimo"
                    val safeDroneName = selectedDrone ?: "Drone Sconosciuto"

                    // 🔹 Salva l'ultimo drone usato
                    getSharedPreferences("prefs", MODE_PRIVATE)
                        .edit()
                        .putString("ultimoDrone", safeDroneName)
                        .apply()

                    val pilotaAttivo = mapOf(
                        "uid" to uid,
                        "name" to safeUserName,
                        "drone" to safeDroneName,
                        "avatarUrl" to avatarUrl
                    )

                    FirebaseFirestore.getInstance().collection("piloti").document(uid).set(pilotaAttivo)
                        .addOnSuccessListener {
                            logDebug(TAG, "✅ Sono in Firebase: Pilota online salvato")

                            // Leggiamo la variabile checklistEnabled dalla raccolta "users"
                            FirebaseFirestore.getInstance().collection("users").document(uid)
                                .get()
                                .addOnSuccessListener { document ->
                                    val checklistEnabled = document.getBoolean("checklistEnabled") ?: false

                                    if (checklistEnabled) {
                                        logDebug(TAG, "✅ Sono in Firebase: CheckList Enabled")
                                        val intent = Intent(this, PreFlightChecklistActivity::class.java)
                                        checklistLauncher.launch(intent)
                                    } else {
                                        logDebug(TAG, "🛫 Premuto Start Flight: $safeUserName - $safeDroneName")
                                        startFlight(safeUserName, safeDroneName)
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Errore nel controllo delle impostazioni", Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Errore nel salvataggio del pilota online", Toast.LENGTH_SHORT).show()
                        }

                }
            }


        // ✅ Listener del pulsante "Start Flight"
        startFlightButton.setOnClickListener {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
            val safeUserName = userName ?: "Pilota Anonimo"
            val safeDroneName = selectedDrone ?: "Drone Sconosciuto"

            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    val checklistEnabled = document.getBoolean("checklistEnabled") ?: false

                    if (checklistEnabled) {
                        // Apri l’activity per la checklist
                        val intent = Intent(this, PreFlightChecklistActivity::class.java)
                        checklistLauncher.launch(intent)
                    } else {
                        // Avvia direttamente il volo
                        logDebug(TAG, "🛫 Premuto Start Flight: $safeUserName - $safeDroneName")
                        startFlight(safeUserName, safeDroneName)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore nel controllo delle impostazioni", Toast.LENGTH_SHORT).show()
                }
        }


        // Apertura News Dronezine
        dronezineButton.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        // Apertura News d-flight
        /* dflightButton.setOnClickListener {
            val url = "https://www.d-flight.it/web-app/"
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(ContextCompat.getColor(this, R.color.midnight_blue))
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(url))
        } */

        droneSkyCheckButton.setOnClickListener {
            val url = "https://droneskycheck-d0136.web.app/"
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setToolbarColor(ContextCompat.getColor(this, R.color.midnight_blue))
                .build()
            customTabsIntent.launchUrl(this, Uri.parse(url))
        }

        //Tasto Stop Flight
        stopFlightButton.setOnClickListener {
            val currentUserName = userName
            if (!currentUserName.isNullOrEmpty()) {
                logDebug(TAG, "Tentativo di eliminare il volo per $currentUserName")
                stopFlight(currentUserName)
            } else {
                Toast.makeText(this, "Non posso fermare un volo inesistente", Toast.LENGTH_SHORT).show()
            }
        }


        //Tasto layers sulla mappa
        layersButton.setOnClickListener {
            val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView = inflater.inflate(R.layout.popup_menu_layout, null)

            val popupWindow = PopupWindow(popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true)

            popupWindow.elevation = 10f

            // Imposta il click listener per le voci di menu
            popupView.findViewById<LinearLayout>(R.id.menu_zones).setOnClickListener {
                zonesVisible = !zonesVisible
                if (zonesVisible) flightZoneLayer.drawZones()
                else flightZoneLayer.clearZones()
                popupWindow.dismiss()
            }

            popupView.findViewById<LinearLayout>(R.id.menu_clouds).setOnClickListener {
                weatherLayerManager.toggleCloudsLayer()
                popupWindow.dismiss()
            }

            popupView.findViewById<LinearLayout>(R.id.menu_wind).setOnClickListener {
                weatherLayerManager.toggleWindLayer()
                popupWindow.dismiss()
            }

            popupView.findViewById<LinearLayout>(R.id.menu_rain).setOnClickListener {
                rainTimestamp?.let {
                    weatherLayerManager.toggleRainLayer(it)
                } ?: Log.w("WeatherLayer", "RainViewer timestamp non ancora pronto")
                popupWindow.dismiss()
            }

            popupView.findViewById<LinearLayout>(R.id.menu_aircraft).setOnClickListener {
                aircraftLayerVisible = !aircraftLayerVisible
                if (aircraftLayerVisible) {
                    aircraftLayer?.start()
                    Toast.makeText(this, "Layer aerei attivo", Toast.LENGTH_SHORT).show()
                } else {
                    aircraftLayer?.stop()
                    Toast.makeText(this, "Layer aerei disattivato", Toast.LENGTH_SHORT).show()
                }
                popupWindow.dismiss()
            }

            // Mostra il popup sotto il pulsante dei layer
            popupWindow.showAsDropDown(layersButton, 0, 0)


        }

        //
        // Tasto Assistente
        //
        voiceBtn.setOnClickListener {
            checkAudioPermissionAndStartListening()
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Parla ora…")
        }

        findViewById<ImageButton>(R.id.closeAssistantOverlay).setOnClickListener {
            findViewById<FrameLayout>(R.id.assistantOverlay).visibility = View.GONE
        }

        findViewById<ImageButton>(R.id.closeMeteoOverlay).setOnClickListener {
            findViewById<FrameLayout>(R.id.meteoOverlay).visibility = View.GONE
        }
        //

        //
        // Tasto full screen
        //
        val fullscreenButton = findViewById<FloatingActionButton>(R.id.fullscreenButton)
        fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }
        //

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


        //vede se si può volare di d-flight JSON
        // Verifica se la TextView esiste nel layout
        findViewById<TextView>(R.id.lowerLimitTextView)?.text = "In attesa...."
        fetchFlightLimitWithLocation()

        // Text to Speach
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }

        //
        // Sezione Drone ID - call back
        //

        // Inizializziamo le mappe per il riconoscimento costruttore e modello di drone
        initDroneMaps()

        droneIdDataManager = OpenDroneIdDataManager(object : OpenDroneIdDataManager.Callback {
            override fun onNewAircraft(obj: AircraftObject) {
                Log.d("DronePilotApp", "🆕 Nuovo drone rilevato: ${obj.connection.value?.macAddress}")
            }

            override fun onLocationUpdate(obj: AircraftObject) {
                //Log.d("DronePilotApp", "📌 ID attuale = ${obj.uasIdString.value} su MAC=${obj.macAddress}")

                val mac = obj.macAddressString
                val location = obj.location.value ?: return

                // Ricava il modello dal Basic ID
                //val uasId = obj.identification1.value?.uasIdAsString
                //val uasId = obj.uasIdString.value
                val uasId = obj.uasIdString.value
                    ?: obj.identification1.value?.uasIdAsString

                val modello = if (!uasId.isNullOrBlank()) {
                    val (manufacturer, model) = parseUasId(uasId, prefixMap, modelMap)
                    if (model != "Modello sconosciuto" && manufacturer != "Costruttore sconosciuto") {
                        "$model ($manufacturer)"
                    } else {
                        uasId
                    }
                } else {
                    "Drone sconosciuto"
                }

                val lat = location.latitude
                val lon = location.longitude
                val alt = location.height.toInt() // Altezza relativa più realistica
                val vel = location.speedHorizontal.toInt()
                val timestamp = System.currentTimeMillis()

                // Filtra valori non validi
                if (lat == 0.0 && lon == 0.0 || alt == -1000 || vel == 255) return

                Log.d("DronePilotApp", "📍 Aggiornamento posizione $mac: lat=$lat, lon=$lon, alt=$alt, vel=$vel, modello=$modello")

                val now = System.currentTimeMillis()
                val lastUpdate = lastUpdateMap[mac] ?: now
                if (now - lastUpdate > 30_000) {
                    Log.d("DronePilotApp", "⚡ Più di 30 secondi senza aggiornamenti: resetto traiettoria per $mac")
                    droneTrajectories[mac]?.remove()
                    droneTrajectories.remove(mac)
                    // 🎨 Cambia colore
                    colorIndex = (colorIndex + 1) % trajectoryColors.size
                }
                lastUpdateMap[mac] = now

                updateDronePosition(mac, lat, lon, alt, vel, modello) // Mostriamo solo il modello nel marker

                // Aggiorna le polilinee del drone ma mano che si sposta
                updatePolyline(mac, lat, lon)

                // 🔥 Aggiorna detected_drones e completed_flights
                processIncomingDroneData(
                    droneId = mac,
                    lat = lat,
                    lon = lon,
                    alt = alt.toDouble(),
                    speed = vel.toDouble(),
                    timestamp = timestamp,
                    model = modello
                )

                // ➕ Aggiunge comunque il punto nella trajectories
                val db = FirebaseFirestore.getInstance()
                db.collection("trajectories")
                    .document(mac)
                    .collection("points")
                    .add(mapOf(
                        "lat" to lat,
                        "lon" to lon,
                        "timestamp" to timestamp
                    ))
                    .addOnSuccessListener {
                        //Log.d("DronePilotApp", "📍 Punto aggiunto a trajectories/$mac")
                    }
                    .addOnFailureListener { e ->
                        Log.e("DronePilotApp", "❌ Errore aggiunta punto in trajectories/$mac: ${e.message}", e)
                    }
            }

            fun processIncomingDroneData(
                droneId: String,
                lat: Double,
                lon: Double,
                alt: Double,
                speed: Double,
                timestamp: Long,
                model: String?
            ) {
                val firestore = FirebaseFirestore.getInstance()
                val detectedDronesRef = firestore.collection("detected_drones")
                val completedFlightsRef = firestore.collection("completed_flights")

                val droneDocRef = detectedDronesRef.document(droneId)

                droneDocRef.get().addOnSuccessListener { snapshot ->
                    val lastTimestamp = snapshot.getLong("timestamp") ?: 0L
                    val timeDiffMillis = timestamp - lastTimestamp

                    //Log.d("DronePilotApp", "🕒 processIncomingDroneData: droneId=$droneId")
                    //Log.d("DronePilotApp", "🕒 lastTimestamp=$lastTimestamp, currentTimestamp=$timestamp, diffMillis=$timeDiffMillis")

                    if (lastTimestamp != 0L && timeDiffMillis > 60 * 60 * 1000) { // Più di 1 ora
                        Log.d("DronePilotApp", "🛬 Più di 1 ora trascorsa. Salvo volo precedente e resetto traiettoria.")

                        // 🛬 Salva fine volo precedente
                        val completedData = mapOf(
                            "droneId" to droneId,
                            "lat" to snapshot.getDouble("lat"),
                            "lon" to snapshot.getDouble("lon"),
                            "altitude" to snapshot.getDouble("altitude"),
                            "speed" to snapshot.getDouble("speed"),
                            "timestamp" to lastTimestamp,
                            "model" to snapshot.getString("model")
                        )

                        completedFlightsRef.add(completedData)
                            .addOnSuccessListener {
                                Log.d("DronePilotApp", "✅ Drone $droneId salvato in completed_flights")
                            }
                            .addOnFailureListener { e ->
                                Log.w("DronePilotApp", "❌ Errore salvataggio completed_flights: ${e.message}")
                            }

                        // 🗑️ Cancella tutti i punti della traiettoria precedente
                        val trajectoryPointsRef = firestore.collection("trajectories").document(droneId).collection("points")

                        trajectoryPointsRef.get().addOnSuccessListener { pointsSnapshot ->
                            for (pointDoc in pointsSnapshot.documents) {
                                pointDoc.reference.delete()
                            }
                            Log.d("DronePilotApp", "🗑️ Traiettoria vecchia cancellata per il drone $droneId")

                            // 🧼 Rimuovi anche la polyline dalla mappa (SOLO SE usi una mappa delle polilinee)
                            droneTrajectories[droneId]?.remove()  // <-- Questo rimuove la Polyline dalla mappa
                            droneTrajectories.remove(droneId)     // <-- Questo rimuove il riferimento dalla Map

                        }.addOnFailureListener { e ->
                            Log.w("DronePilotApp", "❌ Errore cancellazione traiettoria per il drone $droneId: ${e.message}")
                        }
                    } else {
                        //Log.d("DronePilotApp", "⏳ Meno di 1 ora trascorsa o primo volo. Non salvo completed_flights.")
                    }

                    // ✍️ Aggiorna la posizione attuale su detected_drones
                    val updatedData = mapOf(
                        "lat" to lat,
                        "lon" to lon,
                        "altitude" to alt,
                        "speed" to speed,
                        "timestamp" to timestamp,
                        "model" to model
                    )
                    droneDocRef.set(updatedData)
                        .addOnSuccessListener {
                            //Log.d("DronePilotApp", "📍 Drone $droneId aggiornato su detected_drones")
                        }
                        .addOnFailureListener { e ->
                            Log.w("DronePilotApp", "❌ Errore aggiornamento detected_drones: ${e.message}")
                        }
                }.addOnFailureListener { e ->
                    Log.w("DronePilotApp", "❌ Errore lettura detected_drones: ${e.message}")
                }
            }

        })

        bluetoothReceiver = BluetoothReceiver(this, droneIdDataManager)
        wifiAwareReceiver = WifiAwareReceiver(this, droneIdDataManager)
        wifiBeaconReceiver = WifiBeaconReceiver(this, droneIdDataManager, null)
        (application as DronePilotApp).bluetoothReceiver = bluetoothReceiver
        //(application as DronePilotApp).wifiAwareReceiver = wifiAwareReceiver // se lo usi in futuro
        (application as DronePilotApp).wifiBeaconReceiver = wifiBeaconReceiver


        //controllo DRI receiver acceso
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val droneIdEnabled = prefs.getBoolean("droneIdEnabled", false)

        if (droneIdEnabled) {
            driLed.setImageResource(R.drawable.led_on)
        } else {
            driLed.setImageResource(R.drawable.led_off)
        }

        //
        // FINE - Drone ID
        //


        //controllo utenti in GroupChat Realtime database
        val connectedUsersRef = FirebaseDatabase.getInstance().getReference("connectedUsers")

        connectedUsersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val numeroUtentiInChat = snapshot.childrenCount.toInt()
                chatUsersText.text = "In chat: $numeroUtentiInChat"
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("DronePilotApp", "Errore caricamento utenti in chat: ${error.message}")
            }
        })

        //controllo utenti loggati al sistema
        val usersRef = FirebaseFirestore.getInstance().collection("users")
        usersRef.whereEqualTo("online", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("DronePilotApp", "Errore caricamento utenti online: ${error.message}")
                    return@addSnapshotListener
                }

                val numeroUtentiConnessi = snapshot?.size() ?: 0
                onlineUsersText.text = "Online: $numeroUtentiConnessi"
            }

    }


    /// INIZIO FUNZIONI

    //
    // - Inizio CRPC Ricevitore di droni
    //

    private fun ensureCrpcDfIconLoaded(onReady: (BitmapDescriptor) -> Unit) {
        val cached = crpcDfIcon
        if (cached != null) { onReady(cached); return }

        val iconUrl = "https://www.kwos.org/appoggio/droni/dronepilotapp/drone_icon.png"
        val density = resources.displayMetrics.density
        val sizeInPx = (32 * density).toInt()

        Glide.with(this@DashboardActivity)
            .asBitmap()
            .load(iconUrl)
            .override(sizeInPx, sizeInPx)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    crpcDfIcon = BitmapDescriptorFactory.fromBitmap(resource)
                    onReady(crpcDfIcon!!)
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun bitmapDescriptorFromVector(drawableId: Int): com.google.android.gms.maps.model.BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(this, drawableId)!!
        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bm)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bm)
    }

    private fun listenCrpcReceivers() {
        crpcReceiversListener?.remove()
        crpcReceiversListener = FirebaseFirestore.getInstance()
            .collection("crpc_receivers")
            .whereEqualTo("online", true)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener

                for (doc in snap.documents) {
                    val id = doc.id
                    val lat = doc.getDouble("lat") ?: continue
                    val lon = doc.getDouble("lon") ?: continue
                    val fixOk = doc.getBoolean("fix_ok") ?: false
                    val pos = LatLng(lat, lon)
                    crpcLastLatLng[id] = pos

                    val title = "CRPC ${id}"
                    val snippet = if (fixOk) "GPS: FIX OK" else "GPS: no fix"

                    val existing = crpcMarkers[id]
                    if (existing == null) {
                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(pos)
                                .title(title)
                                .snippet(snippet)
                                .icon(bitmapDescriptorFromVector(R.drawable.ic_crpc_antenna)) // <-- tua drawable
                        )
                        marker?.tag = "CRPC:$id" // <-- importantissimo: ci serve per riconoscere il click
                        if (marker != null) crpcMarkers[id] = marker
                    } else {
                        existing.position = pos
                        existing.title = title
                        existing.snippet = snippet
                    }
                }
            }
    }

    private fun listenCrpcAlerts() {
        crpcAlertsListener?.remove()
        // se vuoi filtrare per un receiver specifico, aggiungi .whereEqualTo("receiverId", "crpc-01")
        crpcAlertsListener = FirebaseFirestore.getInstance()
            .collection("crpc_alerts")
            .orderBy("ts_iso", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null || snap.isEmpty) return@addSnapshotListener
                val doc = snap.documents.first()

                val receiverId = doc.getString("receiverId") ?: return@addSnapshotListener
                val radiusM   = (doc.getDouble("radius_m") ?: 0.0)
                val colorHex  = doc.getString("color") ?: "#f2cb05"
                val pos       = crpcLastLatLng[receiverId] ?: return@addSnapshotListener

                // anti-duplicato
                val tsIso = doc.getString("ts_iso") ?: ""
                val tsKey = tsIso.hashCode().toLong()
                if (tsKey == lastCrpcAlertTs) return@addSnapshotListener
                lastCrpcAlertTs = tsKey

                // NEW: bearing/confidence
                val bearingDeg = doc.getDouble("bearing_deg")    // es. 348.2
                val dfConf     = doc.getDouble("df_confidence")  // es. 0.847

                // Mostra ring pulsante (come prima)
                showCrpcRing(pos, radiusM, colorHex)

                // NEW: disegna il settore se abbiamo un bearing valido
                if (bearingDeg != null) {
                    val sweepDeg = confidenceToSweep(dfConf)
                    // Piccola salvaguardia sul raggio, per garantire visibilità
                    val r = if (radiusM < 150.0) 150.0 else radiusM
                    drawSector(pos, r, bearingDeg, sweepDeg, colorHex)
                    showCrpcDfMarker(pos, r , bearingDeg)
                }



                // Salva info per HUD (aggiornato con bearing/conf)
                val info = CrpcAlertInfo(
                    band     = doc.getString("band"),
                    freqMhz  = doc.getDouble("freq_mhz"),
                    label    = doc.getString("label"),
                    rssiDbm  = doc.getDouble("rssi_dbm"),
                    radiusM  = doc.getDouble("radius_m"),
                    tsIso    = tsIso,
                    bearingDeg = bearingDeg,
                    dfConfidence = dfConf
                )
                lastAlertByReceiverId[receiverId] = info

                // Blink come prima
                startCrpcBlink(receiverId = receiverId, periodMs = 450L, ttlMs = 7000L)
            }

    }

    private fun showCrpcDfMarker(center: LatLng, radiusM: Double, bearingDeg: Double) {
        // Garantisce un raggio minimo per visibilità (stessa logica del settore)
        val r = if (radiusM < 150.0) 150.0 else radiusM
        val end = computeOffset(center, r, bearingDeg)

        ensureCrpcDfIconLoaded { icon ->
            // Crea/aggiorna marker piatto ruotato verso il bearing
            if (crpcDfMarker == null) {
                crpcDfMarker = mMap.addMarker(
                    MarkerOptions()
                        .position(end)
                        .title("Direzione segnale")
                        .snippet("Bearing: ${String.format("%.0f°", ((bearingDeg%360)+360)%360)}")
                        .icon(icon)
                        .anchor(0.5f, 0.5f)
                        .flat(true)
                        .rotation(((bearingDeg + 360) % 360).toFloat())
                        .zIndex(2f)
                )
            } else {
                crpcDfMarker!!.position = end
                crpcDfMarker!!.rotation = (((bearingDeg % 360) + 360) % 360).toFloat()
            }
            // opzionale: piccolo “nudge” per rendere evidente l’aggiornamento
            // crpcDfMarker!!.showInfoWindow()
        }
    }


    private fun showCrpcHud(receiverId: String, ttlMs: Long = 8000L) {
        Log.d("DronePilotApp", "CRPC showCrpcHud() → receiverId=$receiverId, info=${lastAlertByReceiverId[receiverId]}")
        val info = lastAlertByReceiverId[receiverId]

        // Band/Freq
        val band = info?.band ?: "—"
        val f = info?.freqMhz?.let { String.format("%.3f MHz", it) } ?: "—"
        hudBandFreq.text = "Band: $band • Freq: $f"

        // Etichetta + NEW: DF su seconda riga
        val dfText = buildString {
            val b = info?.bearingDeg
            val c = info?.dfConfidence
            if (b != null) {
                append("DF: ${String.format("%.0f°", ((b % 360) + 360) % 360)}")
                if (c != null) append("  (${String.format("%.0f%%", (c*100.0))})")
            } else {
                append("DF: —")
            }
        }
        hudLabel.text = "Label: ${info?.label ?: "—"}\n$dfText"

        val rssi = info?.rssiDbm?.let { String.format("%.0f dBm", it) } ?: "—"
        hudRssi.text = "RSSI: $rssi"

        val dist = info?.radiusM?.let {
            if (it >= 1000) String.format("%.1f km", it/1000.0) else String.format("%.0f m", it)
        } ?: "—"
        hudDistance.text = "Distanza stimata: $dist"

        crpcHud.visibility = View.VISIBLE
        hudAutoHideRunnable?.let { crpcHud.removeCallbacks(it) }
        hudAutoHideRunnable = Runnable { hideCrpcHud() }
        crpcHud.postDelayed(hudAutoHideRunnable!!, ttlMs)
        Log.d("DronePilotApp", "CRPC Mostro HUD per $receiverId con info=${lastAlertByReceiverId[receiverId]}")
    }



    private fun showCrpcRing(center: LatLng, radiusMeters: Double, colorHex: String) {
        // chiudi precedente
        crpcRingCircle?.remove()
        crpcRingAnimator?.cancel()

        val argb = Color.parseColor(colorHex)
        crpcRingCircle = mMap.addCircle(
            com.google.android.gms.maps.model.CircleOptions()
                .center(center)
                .radius(radiusMeters)
                .strokeColor(argb)
                .strokeWidth(8f)
                .fillColor(Color.TRANSPARENT)
                .zIndex(0f)
        )

        // animazione "pulse" (raggio 0.9x → 1.05x)
        crpcRingAnimator = ValueAnimator.ofFloat(0.9f, 1.05f).apply {
            duration = 1400L
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val k = it.animatedValue as Float
                crpcRingCircle?.radius = radiusMeters * k
                // leggero “blink” sullo stroke
                val a = (180 + (it.animatedFraction * 60)).toInt().coerceIn(0, 255)
                val r = Color.red(argb)
                val g = Color.green(argb)
                val b = Color.blue(argb)
                crpcRingCircle?.strokeColor = Color.argb(a, r, g, b)
            }
            start()
        }
    }

    private var crpcSector: com.google.android.gms.maps.model.Polygon? = null

    private fun confidenceToSweep(conf: Double?): Double {
        // conf ∈ [0,1] → sweep ∈ [12°, 60°]; più conf, settore più stretto
        val c = (conf ?: 0.0).coerceIn(0.0, 1.0)
        return 12.0 + (1.0 - c) * 48.0
    }

    private fun computeOffset(start: LatLng, distanceMeters: Double, bearingDeg: Double): LatLng {
        val R = 6371000.0
        val δ = distanceMeters / R
        val θ = Math.toRadians(((bearingDeg % 360) + 360) % 360.0)
        val φ1 = Math.toRadians(start.latitude)
        val λ1 = Math.toRadians(start.longitude)

        val sinφ1 = Math.sin(φ1)
        val cosφ1 = Math.cos(φ1)
        val sinδ = Math.sin(δ)
        val cosδ = Math.cos(δ)
        val sinθ = Math.sin(θ)
        val cosθ = Math.cos(θ)

        val sinφ2 = sinφ1 * cosδ + cosφ1 * sinδ * cosθ
        val φ2 = Math.asin(sinφ2)
        val y = sinθ * sinδ * cosφ1
        val x = cosδ - sinφ1 * sinφ2
        val λ2 = λ1 + Math.atan2(y, x)

        return LatLng(Math.toDegrees(φ2), Math.toDegrees(λ2))
    }

    private fun drawSector(center: LatLng, radiusM: Double, bearingDeg: Double, sweepDeg: Double, colorHex: String) {
        crpcSector?.remove()
        val steps = 60
        val points = mutableListOf<LatLng>()
        val start = bearingDeg - sweepDeg / 2.0
        val end   = bearingDeg + sweepDeg / 2.0
        points.add(center)
        for (i in 0..steps) {
            val brg = Math.toRadians(start + (end - start) * i / steps)
            val d = radiusM / 6371000.0 // raggio terrestre
            val lat1 = Math.toRadians(center.latitude)
            val lon1 = Math.toRadians(center.longitude)
            val lat2 = Math.asin(Math.sin(lat1) * Math.cos(d) + Math.cos(lat1) * Math.sin(d) * Math.cos(brg))
            val lon2 = lon1 + Math.atan2(Math.sin(brg) * Math.sin(d) * Math.cos(lat1),
                Math.cos(d) - Math.sin(lat1) * Math.sin(lat2))
            points.add(LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2)))
        }
        val argb = Color.parseColor(colorHex)
        crpcSector = mMap.addPolygon(
            com.google.android.gms.maps.model.PolygonOptions()
                .addAll(points)
                .strokeColor(argb)
                .strokeWidth(6f)
                .fillColor(Color.argb(40, Color.red(argb), Color.green(argb), Color.blue(argb)))
                .zIndex(1f)
        )
    }

    private fun startCrpcBlink(receiverId: String, periodMs: Long = 450L, ttlMs: Long = 6000L) {
        // Se esiste già, riparti da zero
        stopCrpcBlink(receiverId, restoreNormalIcon = false)

        val marker = crpcMarkers[receiverId] ?: return
        crpcBlinkOn[receiverId] = false

        val runnable = object : Runnable {
            override fun run() {
                val on = !(crpcBlinkOn[receiverId] ?: false)
                crpcBlinkOn[receiverId] = on
                val iconRes = if (on) R.drawable.ic_crpc_antenna_alert else R.drawable.ic_crpc_antenna
                marker.setIcon(bitmapDescriptorFromVector(iconRes))
                mainHandler.postDelayed(this, periodMs)
            }
        }
        crpcBlinkRunnables[receiverId] = runnable
        mainHandler.post(runnable)

        // Stop automatico dopo TTL
        mainHandler.postDelayed({ stopCrpcBlink(receiverId) }, ttlMs)
    }

    private fun stopCrpcBlink(receiverId: String, restoreNormalIcon: Boolean = true) {
        crpcBlinkRunnables.remove(receiverId)?.let { mainHandler.removeCallbacks(it) }
        crpcBlinkOn.remove(receiverId)
        if (restoreNormalIcon) {
            crpcMarkers[receiverId]?.setIcon(bitmapDescriptorFromVector(R.drawable.ic_crpc_antenna))
        }
    }

    // Per sicurezza: ferma tutti i blink (es. in onDestroy)
    private fun stopAllCrpcBlinks() {
        crpcBlinkRunnables.values.forEach { mainHandler.removeCallbacks(it) }
        crpcBlinkRunnables.clear()
        crpcBlinkOn.clear()
        // ripristina icone normali
        crpcMarkers.values.forEach { it.setIcon(bitmapDescriptorFromVector(R.drawable.ic_crpc_antenna)) }
    }

    private fun hideCrpcHud() {
        crpcHud.visibility = View.GONE
        hudAutoHideRunnable?.let { crpcHud.removeCallbacks(it) }
        hudAutoHideRunnable = null
    }


    //
    // -- Fine Ricevitore di Droni
    //



    // Controllo scadenza documenti
    private fun checkDocumentExpirations() {
        val TAG = "DocExpiryCheck"
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val alertThresholds = listOf(5, 15, 30, 60)
        val today = Calendar.getInstance()

        Log.d(TAG, "🔍 Inizio controllo scadenze documenti")

        FirebaseFirestore.getInstance()
            .collection("pilotProfiles").document(uid)
            .collection("documents")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val expiryStr = document.getString("expiryDate") ?: continue
                    val title = document.getString("title") ?: "Documento"
                    val docId = document.id

                    try {
                        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val expiryDate = sdf.parse(expiryStr)
                        if (expiryDate == null) {
                            Log.d(TAG, "⛔️ Data non parsabile per $title ($expiryStr)")
                            continue
                        }

                        val millisPerDay = 1000 * 60 * 60 * 24
                        val diffDays = ((expiryDate.time - today.timeInMillis + millisPerDay / 2) / millisPerDay).toInt()
                        Log.d(TAG, "📆 Documento $title → $diffDays giorni alla scadenza")

                        val threshold = alertThresholds.find { diffDays <= it } ?: continue
                        Log.d(TAG, "📄 $title entra nella soglia $threshold")

                        val acknowledged = document.getBoolean("acknowledgedExpiryAlert") ?: false
                        val lastAckDays = document.getLong("lastAcknowledgedDays")?.toInt() ?: Int.MIN_VALUE

                        if (acknowledged && threshold < lastAckDays) {
                            Log.d(TAG, "🔁 Reset acknowledgment (da $lastAckDays a $threshold giorni)")

                            val docRef = FirebaseFirestore.getInstance()
                                .collection("pilotProfiles").document(uid)
                                .collection("documents").document(docId)

                            docRef.update(
                                mapOf(
                                    "acknowledgedExpiryAlert" to false,
                                    "lastAcknowledgedThreshold" to null
                                )
                            ).addOnSuccessListener {
                                Log.d(TAG, "✅ Reset completato. Mostro alert per $title")
                                if (!alertAlreadyShownThisSession) {
                                    alertAlreadyShownThisSession = true
                                    showDocumentExpiryAlert(docId, expiryStr, title)
                                }
                                getSharedPreferences("prefs", MODE_PRIVATE)
                                    .edit().putBoolean("hasExpiringDocs", true).apply()
                            }
                            break
                        }

                        if (!acknowledged) {
                            Log.d(TAG, "⚠️ Documento $title non acknowledged: mostro alert")

                            if (!alertAlreadyShownThisSession) {
                                alertAlreadyShownThisSession = true
                                showDocumentExpiryAlert(docId, expiryStr, title)
                            }

                            getSharedPreferences("prefs", MODE_PRIVATE)
                                .edit().putBoolean("hasExpiringDocs", true).apply()
                            break
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Errore parsing data per $title: ${e.message}")
                    }
                }
            }
    }


    private fun showDocumentExpiryAlert(documentId: String, expiryDate: String, title: String) {
        val intent = Intent(this, DocumentoAlertActivity::class.java).apply {
            putExtra("documentId", documentId)
            putExtra("expiryDate", expiryDate)
            putExtra("documentTitle", title)
        }
        startActivity(intent)
    }



    // Traiettorie di diverso colore per voli droni
    private fun getColorFromHue(hue: Float): Int {
        val hsv = floatArrayOf(hue, 1f, 1f)
        return Color.HSVToColor(hsv)
    }

    private val trajectoryColors = listOf(
        BitmapDescriptorFactory.HUE_BLUE,
        BitmapDescriptorFactory.HUE_GREEN,
        BitmapDescriptorFactory.HUE_ORANGE,
        BitmapDescriptorFactory.HUE_ROSE,
        BitmapDescriptorFactory.HUE_VIOLET,
        BitmapDescriptorFactory.HUE_YELLOW,
        BitmapDescriptorFactory.HUE_CYAN,
        BitmapDescriptorFactory.HUE_MAGENTA
    )
    private var colorIndex = 0

    //
    // Drone ID - Mappe Costruttori - Modelli
    //
    private fun initDroneMaps() {
        prefixMap = mapOf(
            "1581F" to "DJI",
            "1581E" to "DJI",
            "1234A" to "Parrot",
            "5678B" to "Autel",
            "1748F" to "Autel",
            "1748C" to "Autel",
            "1596"  to "Dronetag",
            "2106"  to "TopView Pollicino"
        )

        modelMap = mapOf(
            "1ZP" to "Mavic 2 Pro",
            "163" to "Mavic 2 Pro",
            "1KP" to "Mavic 2 Zoom",
            "0ZP" to "Mavic Air 2",
            "0M6" to "Mavic 2 Zoom",
            "1WN" to "Mavic Air 2",
            "3ZP" to "Phantom 4 Pro V2.0",
            "2ZP" to "Phantom 4 Advanced",
            "3N3" to "Mavic Air 2",
            "5ZP" to "Inspire 2",
            "446" to "Agras T30",
            "4GC" to "Mavic 2E",
            "4ZP" to "Mavic Mini",
            "45T" to "Mavic 3",
            "4QW" to "Avata",
            "4QZ" to "Mavic 3 Cine",
            "4XF" to "Mini 3 Pro",
            "5FJ" to "Mavic 3 Thermal",
            "5YH" to "Mini 3",
            "574" to "Agras T40",
            "6BU" to "Agras T50",
            "6Z9" to "Mini 4 Pro",
            "67P" to "Mavic 3 Classic",
            "67Q" to "Mavic 3 Pro",
            "6N8" to "Air 3",
            "6W8" to "Avata 2",
            "7ZP" to "Air 2S",
            "3YT" to "Air 2S",
            "8ZP" to "Mini 2",
            "895" to "Air 3S",
            "9DE" to "Mini 5 Pro",
            "7FV" to "Matrice 4E",
            "7K3" to "Matrice 4T",
            "8HH" to "Matrice 4D",
            "8HG" to "Matrice 4 TD",
            "986" to "Mavic 4 Pro",

            "JD2" to "Dragonfish Lite",
            "JD3" to "Dragonfish Pro",
            "JD1" to "Dragonfish Std",
            "EV2" to "EVO II V3",
            "EV3" to "EVO Max",
            "EV5" to "EVO Lite",
            "V4A" to "Autel Alpha",

            "A34" to "Beacon"

        )
    }

    private fun parseUasId(uasId: String, prefixMap: Map<String, String>, modelMap: Map<String, String>): Pair<String, String> {
        if (uasId.length < 7) return Pair("Costruttore sconosciuto", "Modello sconosciuto")

        // Cerca il prefisso più lungo corrispondente
        val manufacturerEntry = prefixMap.entries
            .firstOrNull { uasId.startsWith(it.key) }

        val manufacturer = manufacturerEntry?.value ?: "Costruttore sconosciuto"
        val prefixLength = manufacturerEntry?.key?.length ?: 0

        val serialPart = uasId.drop(prefixLength)
        val modelKey = serialPart.take(3).uppercase()

        val model = modelMap[modelKey] ?: "Modello sconosciuto"

        return Pair(manufacturer, model)
    }

    private fun updatePolyline(mac: String, lat: Double, lon: Double) {
        val dronePolyline = droneTrajectories[mac]

        if (dronePolyline == null) {
            // Non esiste ancora, la creiamo nuova colorata
            val polylineOptions = PolylineOptions()
                .add(LatLng(lat, lon))
                .width(5f)
                .color(getColorFromHue(trajectoryColors[colorIndex]))

            val newPolyline = mMap.addPolyline(polylineOptions)
            droneTrajectories[mac] = newPolyline
        } else {
            // Esiste già -> aggiorniamo
            val points = dronePolyline.points.toMutableList()
            points.add(LatLng(lat, lon))
            dronePolyline.points = points
        }
    }


    //
    // FINE:  Drone ID - Mappe Costruttori - Modelli
    //

    // Mostra il menu in Popup
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        popupMenu.menuInflater.inflate(R.menu.menu_options, popupMenu.menu)

        // Controllo scadenza documenti
        // Controlla se c'è un documento in scadenza segnalato nei preferences
        val hasExpiring = getSharedPreferences("prefs", MODE_PRIVATE)
            .getBoolean("hasExpiringDocs", false)

        if (hasExpiring) {
            val itemImpostazioni = popupMenu.menu.findItem(R.id.menu_impostazioni)
            itemImpostazioni?.title = "⚙️ Impostazioni ✳️"
        }


        // 🔄 Cambia il titolo della voce Group Chat se ci sono nuovi messaggi
        if (nuovoMessaggioGruppoPresente) {
            val groupChatItem = popupMenu.menu.findItem(R.id.menu_group_chat)
            groupChatItem.title = "💬 Group Chat ✳️"
            // oppure: "💬 Group Chat (1)", o cambia icona con un'altra temporanea
        }

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_piloti_online -> {
                    startActivity(Intent(this, PilotiOnlineActivity::class.java))
                    true
                }
                R.id.menu_impostazioni -> {
                    //startActivity(Intent(this, ImpostazioniActivity::class.java))
                    settingsLauncher.launch(Intent(this, ImpostazioniActivity::class.java))
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
                R.id.menu_drone_log -> {
                    startActivity(Intent(this, DroneLogActivity::class.java))
                    true
                }
                R.id.menu_nd_filter -> {
                    startActivity(Intent(this, NDFilterAssistantActivity::class.java))
                    true
                }
                R.id.menu_quiz -> {
                    val intent = Intent(this, QuizHomeActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.misuratore_distanze -> {
                    val intent = Intent(this, ImageDistanceActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()

    }

    // Listener per vedere se vi sono nuovi messaggi nell Group Chat
    //
    private fun isGroupChatActivityOpen(): Boolean {
        return GroupChatActivity.isOpen
    }


    private fun listenForGroupChatNotifications() {
        val messagesRef = FirebaseDatabase.getInstance().reference.child("groupchat")

        messagesRef.limitToLast(1).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d("DronePilotApp", "GroupChatNotif: 📩 Nuovo messaggio rilevato")

                if (!isGroupChatActivityOpen()) {
                    nuovoMessaggioGruppoPresente = true
                    Log.d("DronePilotApp", "GroupChatNotif: 🔔 Chat non aperta, avvio lampeggio")
                    flashMenuButton()
                } else {
                    Log.d("DronePilotApp", "GroupChatNotif: ✅ Chat già aperta, niente lampeggio")
                }
            }


            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Fa flashare l'icona del Menu in caso di presenza di messaggi nella Group Chat
    // 👉 Variabile di istanza
    var flashingAnimator: ObjectAnimator? = null

    // 🔄 Metodo di istanza: usabile dentro DashboardActivity
    private fun flashMenuButton() {
        val menuButton = findViewById<ImageButton>(R.id.menuButton)

        if (flashingAnimator == null) {
            flashingAnimator = ObjectAnimator.ofFloat(menuButton, "alpha", 1f, 0.3f, 1f).apply {
                duration = 600
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                start()
            }
            Log.d("DronePilotApp", "MenuFlash: ✨ Flash avviato.")
        }
    }

    // 🧭 Companion per accesso statico da fuori Activity
    companion object {
        // 🔔 Flag per messaggi non letti in group chat
        var nuovoMessaggioGruppoPresente: Boolean = false

        // 🔁 Istanza viva di DashboardActivity
        var currentInstance: DashboardActivity? = null

        // 🛑 Metodo per fermare l'animazione del menu
        fun stopFlashingMenuButton() {
            currentInstance?.runOnUiThread {
                currentInstance?.stopFlashing()
            }
        }
    }


    fun stopFlashing() {
        flashingAnimator?.cancel()
        flashingAnimator = null
        findViewById<ImageButton>(R.id.menuButton)?.alpha = 1f
        Log.d("DronePilotApp", "MenuFlash: 🔕 Flash arrestato.")
    }




    //

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

        try {
            val success = googleMap.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this, R.raw.dark_map_style)
            )
            if (!success) {
                Log.e("DronePilotApp", "MapStyle: Stile mappa non applicato correttamente.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("DronePilotApp", "MapStyle: File di stile non trovato.", e)
        }

        // click per fare ricerche
        mMap.setOnMapClickListener { latLng ->
            // 🔁 Rimuovi il vecchio marker se esiste
            searchMarker?.remove()

            // 📍 Aggiungi nuovo marker
            searchMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Zona selezionata")
            )?.apply {
                tag = "search"  // 👈 Aggiungiamo un tag per riconoscerlo
            }

            // 🔄 Sposta la mappa (opzionale, se vuoi animare)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 8f))

            // 🔍 Fai la richiesta come se fosse una ricerca
            fetchFlightLimit(latLng.latitude, latLng.longitude) { lowerLimit ->
                val textView = findViewById<TextView>(R.id.lowerLimitTextView)
                textView.text = "Open Category fino a: $lowerLimit m"
                textView.visibility = View.VISIBLE
                textView.setTextColor(getColor(R.color.white))

                Toast.makeText(this, "Open Category fino a: $lowerLimit m", Toast.LENGTH_LONG).show()

                // 👉 INVIO A WEAR
                WearMessageSender.send(
                    this,
                    "/status",
                    "Open fino a ${lowerLimit} m"
                )

                Wearable.getNodeClient(this).connectedNodes
                    .addOnSuccessListener { nodes ->
                        nodes.forEach {
                            Log.d(
                                "DronePilotApp",
                                "Node id=${it.id}, name=${it.displayName}, isNearby=${it.isNearby}"
                            )
                        }
                    }

            }
        }

        // ✅ Unico listener marker (più robusto)
        mMap.setOnMarkerClickListener { marker ->
            val tag = marker.tag as? String
            Log.d("DronePilotApp", "MarkerClick → title=${marker.title} tag=$tag")

            when {
                // Marker "ricerca" → rimuovi
                tag == "search" -> {
                    marker.remove()
                    searchMarker = null
                    Toast.makeText(this, "📍 Zona cercata rimossa", Toast.LENGTH_SHORT).show()
                    true
                }

                // ⛳️ Marker CRPC → HUD
                tag?.startsWith("CRPC:") == true -> {
                    val id = tag.removePrefix("CRPC:")
                    marker.hideInfoWindow()          // evita la card "CRPC crpc-01 / GPS..."
                    showCrpcHud(id, 8000L)           // questa versione logga
                    true
                }

                // Altri marker (piloti, droni…) → InfoWindow auto-hide
                else -> {
                    marker.showInfoWindow()
                    Handler(Looper.getMainLooper()).postDelayed({
                        try { marker.hideInfoWindow() } catch (_: Exception) {}
                    }, 4000)
                    true
                }
            }
        }



        // Info windows
        mMap.setOnInfoWindowClickListener { marker ->
            val id = marker.tag as? String
            if (id != null && pilotMarkers.containsKey(id)) {   // ✅ solo piloti
                openChatWithPilot(id)
            }
        }
        //mMap.setOnInfoWindowClickListener { marker ->
        //    val userId = marker.tag as? String
        //    if (userId != null) {
        //        openChatWithPilot(userId)
        //    }
        //}



        // Permette di fare un InfoWindow che gestisce gli a capo
        // Infowindow con bordi smussati
        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoWindow(marker: Marker): View? {
                return null
            }

            override fun getInfoContents(marker: Marker): View {
                val context = this@DashboardActivity

                // Crea un CardView per il bordo smussato
                val cardView = CardView(context).apply {
                    radius = 16f * context.resources.displayMetrics.density // Angoli smussati
                    setCardBackgroundColor(Color.WHITE)
                    cardElevation = 8f * context.resources.displayMetrics.density
                    useCompatPadding = true
                    setContentPadding(16, 16, 16, 16)
                }

                // Layout interno verticale
                val layout = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                }

                // Titolo (nome drone o pilota)
                val title = TextView(context).apply {
                    text = marker.title
                    setTextColor(Color.BLACK)
                    setTypeface(null, Typeface.BOLD)
                    textSize = 16f
                }

                // Snippet (info extra)
                val snippet = TextView(context).apply {
                    text = marker.snippet
                    setTextColor(Color.DKGRAY)
                    textSize = 14f
                }

                layout.addView(title)
                layout.addView(snippet)

                cardView.addView(layout)

                return cardView
            }
        })


        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            mMap.isMyLocationEnabled = true
            loadPilots()
            listenForChatAvailability()
            loadDrones()
            listenCrpcReceivers()
            listenCrpcAlerts()
            // Layer Flight Zone
            flightZoneLayer = FlightZoneLayer(this, googleMap)
            // Layer Meteo
            weatherLayerManager = WeatherLayerManager(googleMap)
            getLatestRainTimestamp() // fetch asincrono
            aircraftLayer = AircraftLayer(this, googleMap)

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

                    db.collection("piloti").document(userId)
                        .get()
                        .addOnSuccessListener { pilotDoc ->
                            if (pilotDoc.exists()) {
                                val lat = pilotDoc.getDouble("latitude")
                                val lng = pilotDoc.getDouble("longitude")
                                val name = pilotDoc.getString("name") ?: "Sconosciuto"
                                val drone = pilotDoc.getString("drone") ?: "N/D"
                                val radioPMR = userDoc.getBoolean("radioPMR") ?: false
                                val availableForChat = userDoc.getBoolean("availableForChat") ?: false

                                if (lat != null && lng != null) {
                                    val position = LatLng(lat, lng)

                                    val snippetText = buildString {
                                        if (availableForChat) {
                                            append("💬 Chat ON")
                                        } else {
                                            append("❌ Chat OFF")
                                        }
                                        if (radioPMR) {
                                            append("\n📻 PMR CH4")
                                        }
                                    }

                                    val iconUrl = when {
                                        radioPMR -> "https://www.kwos.org/appoggio/droni/dronepilotapp/marker_pmr_mini.png"
                                        availableForChat -> "https://www.kwos.org/appoggio/droni/dronepilotapp/marker_android_mini.png"
                                        else -> "https://www.kwos.org/appoggio/droni/dronepilotapp/marker_drone_mini.png"
                                    }

                                    // Carica immagine da URL e crea marker
                                    val density = resources.displayMetrics.density
                                    val sizeInPx = (32 * density).toInt()

                                    Glide.with(this@DashboardActivity)
                                        .asBitmap()
                                        .load(iconUrl)
                                        .override(sizeInPx, sizeInPx)
                                        .into(object : CustomTarget<Bitmap>() {
                                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                                val customIcon = BitmapDescriptorFactory.fromBitmap(resource)

                                                val markerOptions = MarkerOptions()
                                                    .position(position)
                                                    .title("$name - $drone")
                                                    .snippet(snippetText)
                                                    .icon(customIcon)

                                                val existingMarker = pilotMarkers[userId]
                                                if (existingMarker == null) {
                                                    val marker = mMap.addMarker(markerOptions)!!
                                                    marker.tag = userId
                                                    pilotMarkers[userId] = marker
                                                    logDebug(TAG, "✅ Marker aggiunto per $userId")
                                                } else {
                                                    existingMarker.position = position
                                                    existingMarker.setIcon(customIcon)
                                                    existingMarker.snippet = snippetText
                                                    logDebug(TAG, "✅ Marker aggiornato per $userId")
                                                }
                                            }

                                            override fun onLoadCleared(placeholder: Drawable?) {
                                                // Optional: gestione del caso in cui l'immagine venga cancellata
                                            }
                                        })
                                } else {
                                    pilotMarkers[userId]?.remove()
                                    pilotMarkers.remove(userId)
                                    logDebug(TAG, "❌ Marker rimosso per $userId")
                                }
                            } else {
                                logWarning(TAG, "⚠️ loadPilots: Nessun dato trovato in 'piloti' per $userId verrà rimesso in inVolo:false dal server")
                            }
                        }
                        .addOnFailureListener { err ->
                            logWarning(TAG, "❌ loadPilots: Errore nel recupero delle coordinate per $userId", err)
                        }
                    logDebug(TAG, "✅ loadPilots: Piloti caricati, impostazione di pilotsLoaded a true")
                    pilotsLoaded = true
                }

            }
    }


    //
    // Utility per Layer Meteo
    //
    private fun getLatestRainTimestamp() {
        val url = "https://tilecache.rainviewer.com/api/maps.json"
        val requestQueue = Volley.newRequestQueue(this)

        val jsonArrayRequest = JsonArrayRequest(Request.Method.GET, url, null,
            { response ->
                val lastTimestamp = response.getString(response.length() - 1)
                rainTimestamp = lastTimestamp
            },
            { error ->
                Log.e("RainViewer", "Errore nel caricamento: ${error.message}")
            })

        requestQueue.add(jsonArrayRequest)
    }
    //
    //
    //


    //
    // Sezione Drone ID
    //
    private fun loadDrones() {
        //val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        //val droneIdEnabled = prefs.getBoolean("droneIdEnabled", false)

        //if (!droneIdEnabled) return

        val db = FirebaseFirestore.getInstance()

        // 📡 1) Carica i droni attivi dalla raccolta "detected_drones"
        db.collection("detected_drones")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val droneId = document.id
                    val lat = document.getDouble("lat") ?: 0.0
                    val lon = document.getDouble("lon") ?: 0.0
                    val alt = document.getLong("altitude")?.toInt() ?: 0
                    val vel = document.getDouble("speed") ?: 0.0
                    val modello = document.getString("model") ?: "Modello sconosciuto"

                    val pos = LatLng(lat, lon)

                    if (droneMarkers.containsKey(droneId)) {
                        droneMarkers[droneId]?.position = pos
                        droneMarkers[droneId]?.snippet = "ID: $droneId\nAlt: $alt m\nVel: $vel m/s"
                    } else {
                        val iconUrl = "https://www.kwos.org/appoggio/droni/dronepilotapp/drone_icon.png"

                        val density = resources.displayMetrics.density
                        val sizeInPx = (32 * density).toInt()
                        Glide.with(this@DashboardActivity)
                            .asBitmap()
                            .load(iconUrl)
                            .override(sizeInPx, sizeInPx)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    val customIcon = BitmapDescriptorFactory.fromBitmap(resource)

                                    val marker = mMap.addMarker(
                                        MarkerOptions()
                                            .position(pos)
                                            .title(modello)
                                            .snippet("ID: $droneId\nAlt: $alt m\nVel: $vel m/s")
                                            .icon(customIcon)
                                    )
                                    if (marker != null) {
                                        droneMarkers[droneId] = marker
                                    }
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // opzionale: gestione in caso di clear
                                }
                            })
                    }


                    // 📈 Carica la traiettoria del drone attivo
                    db.collection("trajectories")
                        .document(droneId)
                        .collection("points")
                        .orderBy("timestamp")
                        .get()
                        .addOnSuccessListener { trajectoryResult ->
                            val points = mutableListOf<LatLng>()
                            for (pointDocument in trajectoryResult) {
                                val pointLat = pointDocument.getDouble("lat") ?: 0.0
                                val pointLon = pointDocument.getDouble("lon") ?: 0.0
                                points.add(LatLng(pointLat, pointLon))
                            }

                            val SEGMENT_TIMEOUT = 120_000L // 2 minuti

                            if (trajectoryResult.documents.isNotEmpty()) {
                                var lastTimestamp: Long? = null
                                var segmentPoints = mutableListOf<LatLng>()

                                for (pointDocument in trajectoryResult) {
                                    val pointLat = pointDocument.getDouble("lat") ?: continue
                                    val pointLon = pointDocument.getDouble("lon") ?: continue
                                    val timestamp = pointDocument.getLong("timestamp") ?: continue
                                    val point = LatLng(pointLat, pointLon)

                                    if (lastTimestamp != null) {
                                        val timeDiff = timestamp - lastTimestamp!!
                                        if (timeDiff > SEGMENT_TIMEOUT) {
                                            // 🧹 Più di 30 secondi: disegna la traiettoria precedente
                                            if (segmentPoints.isNotEmpty()) {
                                                mMap.addPolyline(
                                                    PolylineOptions()
                                                        .addAll(segmentPoints)
                                                        .color(getColorFromHue(trajectoryColors[colorIndex]))
                                                        .width(5f)
                                                )
                                                // 🎨 Cambia colore per il prossimo segmento
                                                colorIndex = (colorIndex + 1) % trajectoryColors.size
                                                segmentPoints.clear()
                                            }
                                        }
                                    }

                                    segmentPoints.add(point)
                                    lastTimestamp = timestamp
                                }

                                // Disegna l'ultimo segmento rimasto
                                if (segmentPoints.isNotEmpty()) {
                                    mMap.addPolyline(
                                        PolylineOptions()
                                            .addAll(segmentPoints)
                                            .color(getColorFromHue(trajectoryColors[colorIndex]))
                                            .width(5f)
                                    )
                                    colorIndex = (colorIndex + 1) % trajectoryColors.size
                                }
                            }

                        }
                        .addOnFailureListener { e ->
                            Log.e("DronePilotApp", "Errore nel caricamento delle traiettorie per il drone $droneId: $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("DronePilotApp", "Errore nel caricamento dei droni: $e")
            }

        // 🛬 2) Carica i droni atterrati dalla raccolta "completed_flights"
        db.collection("completed_flights")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val flightId = document.id
                    val lat = document.getDouble("lat") ?: 0.0
                    val lon = document.getDouble("lon") ?: 0.0
                    val modello = document.getString("model") ?: "Modello sconosciuto"

                    val pos = LatLng(lat, lon)

                    if (!droneMarkers.containsKey(flightId)) {
                        val iconUrl = "https://www.kwos.org/appoggio/droni/dronepilotapp/drone_icon_landed.png"

                        val density = resources.displayMetrics.density
                        val sizeInPx = (32 * density).toInt()
                        Glide.with(this@DashboardActivity)
                            .asBitmap()
                            .load(iconUrl)
                            .override(sizeInPx, sizeInPx)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    val customIcon = BitmapDescriptorFactory.fromBitmap(resource)

                                    val marker = mMap.addMarker(
                                        MarkerOptions()
                                            .position(pos)
                                            .title("$modello (Atterrato)")
                                            .snippet("Volo completato")
                                            .icon(customIcon)
                                    )
                                    if (marker != null) {
                                        droneMarkers[flightId] = marker
                                    }
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // opzionale: gestione del clear
                                }
                            })
                    }

                }
            }
            .addOnFailureListener { e ->
                Log.e("DronePilotApp", "Errore nel caricamento dei droni atterrati: $e")
            }
    }


    private val droneDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val droneId = intent.getStringExtra("droneId")
            val lat = intent.getDoubleExtra("lat", 0.0)
            val lon = intent.getDoubleExtra("lon", 0.0)
            val altitude = intent.getIntExtra("altitude", 0)
            val speed = intent.getIntExtra("speed", 0)
            val modello = intent.getStringExtra("modello") ?: "Drone sconosciuto"

            // Aggiorna la mappa con i dati ricevuti
            updateDronePosition(droneId.toString(), lat, lon, altitude, speed, modello)
        }
    }

    // Funzione per aggiungere o aggiornare un marker del drone sulla mappa
    fun updateDronePosition(droneId: String?, lat: Double, lon: Double, altitude: Int, speed: Int, modello: String) {
        val id = droneId ?: "Drone sconosciuto"
        val position = LatLng(lat, lon)

        // Controlla se il drone è già sulla mappa
        val existingMarker = droneMarkers[id]

        if (existingMarker != null) {
            // Se il marker esiste già, aggiorna la sua posizione
            existingMarker.position = position
            existingMarker.title = modello
            existingMarker.snippet = "ID: $droneId\nAlt.: $altitude m\nVel.: $speed m/s"
        } else {
            val iconUrl = "https://www.kwos.org/appoggio/droni/dronepilotapp/drone_icon.png"

            val density = resources.displayMetrics.density
            val sizeInPx = (32 * density).toInt() // Qui decidi tu quanto grande deve essere, ad es. 48dp

            Glide.with(this@DashboardActivity)
                .asBitmap()
                .load(iconUrl)
                .override(sizeInPx, sizeInPx)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val customIcon = BitmapDescriptorFactory.fromBitmap(resource)

                        val marker = mMap.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title(modello)
                                .snippet("ID: $droneId\nAlt.: $altitude m\nVel.: $speed m/s")
                                .icon(customIcon)
                        )

                        if (marker != null) {
                            droneMarkers[id] = marker
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // opzionale: gestione se l'immagine viene cancellata
                    }
                })
        }


        // Aggiungi il punto alla traiettoria
        val trajectory = droneTrajectories[id]
        if (trajectory != null) {
            // Aggiungi il punto alla polilinea esistente
            trajectory.points.add(LatLng(lat, lon))
        } else {
            // Se non esiste, crea una nuova polilinea
            val newTrajectory = mMap.addPolyline(
                PolylineOptions()
                    .add(LatLng(lat, lon))
                    .color(Color.BLUE)
                    .width(5f)
            )
            droneTrajectories[id] = newTrajectory
        }
    }
    //
    // FINE - Sezione Drone ID
    //


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
                    val radioPMR = doc.getBoolean("radioPMR") ?: false
                    val inVolo = doc.getBoolean("inVolo") ?: false // Recupera lo stato di volo

                    // Evita di processare i marker di chi non è in volo
                    if (!inVolo) {
                        //logDebug(TAG, "🚫 Chat: $userId non è in volo, marker ignorato")
                        return@forEach
                    }

                    // Da rimuovere questo log quando gli utenti saranno tanti
                    logDebug(TAG, "🔄 ChatAvail: Stato aggiornato per $userId: $availableForChat")

                    // Recuperiamo il marker e aggiorniamo l'icona e lo snippet
                    val existingMarker = pilotMarkers[userId]
                    if (existingMarker != null) {
                        val iconUrl = when {
                            radioPMR -> "https://www.kwos.org/appoggio/droni/dronepilotapp/marker_pmr_mini.png"
                            availableForChat -> "https://www.kwos.org/appoggio/droni/dronepilotapp/marker_android_mini.png"
                            else -> "https://www.kwos.org/appoggio/droni/dronepilotapp/marker_drone_mini.png"
                        }

                        val density = resources.displayMetrics.density
                        val sizeInPx = (32 * density).toInt() // 48dp dinamico

                        Glide.with(this@DashboardActivity)
                            .asBitmap()
                            .load(iconUrl)
                            .override(sizeInPx, sizeInPx)
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    val customIcon = BitmapDescriptorFactory.fromBitmap(resource)
                                    existingMarker.setIcon(customIcon)

                                    val snippetText = buildString {
                                        if (availableForChat) {
                                            append("💬 Chat ON")
                                        } else {
                                            append("❌ Chat OFF")
                                        }
                                        if (radioPMR) {
                                            append("\n📻 PMR CH4")
                                        }
                                    }
                                    existingMarker.snippet = snippetText
                                    logDebug(TAG, "✅ ChatAvail: Marker aggiornato per $userId")
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // gestione opzionale
                                }
                            })
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


                val iconUrl = "https://www.kwos.org/appoggio/droni/dronepilotapp/marker_drone_mini.png"

                val density = resources.displayMetrics.density
                val sizeInPx = (32 * density).toInt() // oppure la dimensione che preferisci

                Glide.with(this@DashboardActivity)
                    .asBitmap()
                    .load(iconUrl)
                    .override(sizeInPx, sizeInPx)
                    .into(object : CustomTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val customIcon = BitmapDescriptorFactory.fromBitmap(resource)

                            val marker = mMap.addMarker(
                                MarkerOptions()
                                    .position(userPosition)
                                    .title("$userName - $droneName")
                                    .icon(customIcon)
                            )

                            if (marker != null) {
                                pilotMarkers[userId] = marker
                                logDebug(TAG, "✅ Marker personale creato per utente $userId")
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {
                            // opzionale: gestione del clear
                        }
                    })


                //pilotMarkers[userId] = marker  // Salva il marker

                logDebug(TAG, "✅ startFlight: Attivato il volo per: $userId - $userName - $droneName")
                startLocationUpdates(userId, userName, droneName)

                // Controlliamo se ha lo stato di availableForChat
                val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

                userRef.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        val inVolo = document.getBoolean("inVolo") ?: false
                        val availableForChat = document.getBoolean("availableForChat") ?: false
                        val radioPMR = document.getBoolean("radioPMR") ?: false

                        if (inVolo) {
                            aggiornaMarker(userId, availableForChat, radioPMR)
                            logDebug(TAG, "🚀 startFlight: Aggiorno marker per $userId con PMR=$radioPMR")
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
                    //Toast.makeText(this, "stopFlight: Nessuna posizione trovata per $cleanedUserName", Toast.LENGTH_SHORT).show()
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
                    // Fa lo zoom sulla posizione del pilota
                    // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 8f))
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

    private fun aggiornaMarker(userId: String, availableForChat: Boolean, radioPMR: Boolean) {
        logDebug(TAG, "✅ aggiornaMarker: sono in aggiornaMarker")
        val existingMarker = pilotMarkers[userId]
        if (existingMarker != null) {
            val iconUrl = when {
                radioPMR -> "https://www.kwos.org/appoggio/droni/dronepilotapp/marker_pmr_mini.png"
                availableForChat -> "https://www.kwos.org/appoggio/droni/dronepilotapp/marker_android_mini.png"
                else -> "https://www.kwos.org/appoggio/droni/dronepilotapp/marker_drone_mini.png"
            }

            val density = resources.displayMetrics.density
            val sizeInPx = (32 * density).toInt() // adesso icona ridotta, più proporzionata

            Glide.with(this@DashboardActivity)
                .asBitmap()
                .load(iconUrl)
                .override(sizeInPx, sizeInPx)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        val customIcon = BitmapDescriptorFactory.fromBitmap(resource)
                        existingMarker.setIcon(customIcon)

                        val snippetText = buildString {
                            if (availableForChat) {
                                append("💬 Chat ON")
                            } else {
                                append("❌ Chat OFF")
                            }
                            if (radioPMR) {
                                append("\n📻 PMR CH4")
                            }
                        }
                        existingMarker.snippet = snippetText
                        logDebug(TAG, "✅ Marker aggiornato per $userId")
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        // opzionale: gestione del clear
                    }
                })
        } else {
            logWarning(TAG, "⚠️ Nessun marker trovato per $userId")
        }
    }


    // Salva lo stato di online per contare il numero di utenti connessi al sistema
    fun saveOnlineStatus(isOnline: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        userRef.update("online", isOnline)
            .addOnSuccessListener {
                Log.d(TAG, "saveOnlineStatus: Stato online aggiornato a $isOnline")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "saveOnlineStatus: Errore aggiornamento stato online", e)
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
                saveOnlineStatus(false) // mette a true lo stato di online
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
        // dichiaro Istanza per ricezione messaggi Group Chat
        DashboardActivity.currentInstance = this
        //
        val user = FirebaseAuth.getInstance().currentUser
        leggiLogin()
        checkDocumentExpirations()


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
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val inVolo = document.getBoolean("inVolo") ?: false
                val availableForChat = document.getBoolean("availableForChat") ?: false
                val radioPMR = document.getBoolean("radioPMR") ?: false

                if (inVolo) {
                    aggiornaMarker(userId, availableForChat, radioPMR)
                    logDebug(TAG, "🚀 onStart Dashboard: Ripristinato stato di volo per $userId")
                } else {
                    logDebug(TAG, "⚠️ onStart Dashboard: Il pilota $userId non era in volo")
                }
            }
        }.addOnFailureListener { e ->
            logError(TAG, "❌ onStart Dashboard: Errore nel recupero dello stato di volo: ${e.message}")
        }
        // Broadcast receiver message
        if (!isMessageReceiverRegistered) {
            val filter = IntentFilter("com.kwos.dronepilotapp.NEW_MESSAGE")
            registerReceiver(messageReceiver, filter, Context.RECEIVER_EXPORTED)
            isMessageReceiverRegistered = true
            Log.d("DronePilotApp", "onStart Dashboard: MessageReceiver registrato in onStart()")
        }

        // Drone ID receiver broadcast

        if (!isDroneReceiverRegistered) {
            val droneDataFilter = IntentFilter("com.example.DRONE_DATA")

            // Usa sempre la nuova API con flag di sicurezza
            registerReceiver(
                droneDataReceiver,
                droneDataFilter,
                Context.RECEIVER_NOT_EXPORTED
            )

            isDroneReceiverRegistered = true
            Log.d("DronePilotApp", "onStart Dashboard: DroneDataReceiver registrato (not exported)")
        }


        // Attiva i receiver per il rilevamento Drone ID se abilitato dalle impostazioni
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        val droneIdEnabled = prefs.getBoolean("droneIdEnabled", false)
        val driLed = findViewById<ImageView>(R.id.driLed)

        if (droneIdEnabled) {
            driLed.setImageResource(R.drawable.led_on)
        } else {
            driLed.setImageResource(R.drawable.led_off)
        }

        if (droneIdEnabled) {
            val hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasBluetoothScan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else true
            val hasNearbyWifi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
            } else true

            if (hasLocation && hasBluetoothScan && hasNearbyWifi) {
                // Uso questo al posto di bluetoothReceiver?.startScanning()
                ContextCompat.startForegroundService(this, Intent(this, BluetoothScanService::class.java))
                //wifiAwareReceiver?.startSession()
                // Uso questo al posto di  wifiBeaconReceiver?.startScan()
                ContextCompat.startForegroundService(this, Intent(this, WifiBeaconScanService::class.java))
                //
                Log.d("DronePilotApp", "onStart Dashboard: Receiver Drone ID attivati")
            } else {
                Log.w("DronePilotApp", "onStart Dashboard: Permessi insufficienti per avviare i receiver")
            }
        } else {
            // Uso questo al posto di bluetoothReceiver?.stopScanning()
            stopService(Intent(this, BluetoothScanService::class.java))
            //wifiAwareReceiver?.stopSession()
            // Uso questo al posto di  wifiBeaconReceiver?.stopScan()
            stopService(Intent(this, WifiBeaconScanService::class.java))
            //

            Log.d("DronePilotApp", "onStart Dashboard: Drone ID disattivato nelle preferenze")
        }


    }

    private fun recuperaDatiPilota() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val userId = user.uid
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val inVolo = document.getBoolean("inVolo") ?: false
                val availableForChat = document.getBoolean("availableForChat") ?: false
                val radioPMR = document.getBoolean("radioPMR") ?: false

                if (inVolo) {
                    aggiornaMarker(userId, availableForChat, radioPMR)
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
                                text = "Open Category fino a: $lowerLimit m"
                                visibility = View.VISIBLE
                                setTextColor(getColor(R.color.white))
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
                    text = "Open Category fino a: $lowerLimit m"
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


    fun writeStackTraceToFile(stackTrace: String) {
        // Ottieni il percorso della memoria interna del dispositivo
        val file = File(filesDir, "stacktrace_log.txt")
        try {
            // Aggiungi lo stacktrace al file
            val outputStream = FileOutputStream(file, true) // 'true' per appendere al file esistente
            outputStream.write(stackTrace.toByteArray())
            outputStream.close()

            // Log per confermare che il file è stato scritto
            logDebug(TAG, "Stacktrace scritto su file: ${file.absolutePath}")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        // Ferma l'istanza per il flash del menù in caso di messaggi nella Group Chat
        DashboardActivity.currentInstance = null

        // Ferma gli aggiornamenti della posizione quando l'attività è in stop
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        // Message broadcast receiver
        //if (isMessageReceiverRegistered) {
        //    try {
        //        unregisterReceiver(messageReceiver)
        //        Log.d("DronePilotApp", "DashboardActivity: MessageReceiver deregistrato in onStop()")
        //    } catch (e: IllegalArgumentException) {
        //        Log.e("DronePilotApp", "DashboardActivity: Errore nella deregistrazione del receiver: ${e.message}")
        //    }
        //    isMessageReceiverRegistered = false
        //}
    }

    override fun onPause() {
        super.onPause()
        // Message broadcast receiver
        //if (isMessageReceiverRegistered) {
        //    try {
        //        unregisterReceiver(messageReceiver)
        //        Log.d("DronePilotApp", "DashboardActivity: MessageReceiver deregistrato in onPause()")
        //    } catch (e: IllegalArgumentException) {
        //        Log.e("DronePilotApp", "DashboardActivity: Errore nella deregistrazione del receiver: ${e.message}")
        //    }
        //    isMessageReceiverRegistered = false
        //}

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

        if (::mMap.isInitialized) {
            loadDrones()  // ✅ Ora sempre valido, a prescindere dallo switch
        }

        checkForNewMessages() // Controlla se ci sono nuovi messaggi
        val userId = auth.currentUser?.uid
        if (userId != null) {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    val inVolo = document.getBoolean("inVolo") ?: false
                    val userName = document.getString("fullName")
                    val droneName = document.getString("drone")

                    if (inVolo && userName != null && droneName != null) {
                        logDebug(TAG, "onResume: faccio partire startLocationUpdates per $userId - $userName - $droneName")
                        startLocationUpdates(userId, userName, droneName)
                    } else {
                        logDebug(TAG, "onResume: inVolo è false o dati mancanti, non avvio startLocationUpdates")
                    }
                }
                .addOnFailureListener {
                    logDebug(TAG, "onResume: Errore nel recupero dati utente: ${it.message}")
                }
        }
        // Ricarica la lista dei piloti quando l'app torna in primo piano, utilizzando l'handler
        handler.post(refreshRunnable) // Avvia il refresh quando l'app torna attiva

    }

    override fun onDestroy() {
        super.onDestroy()
        logDebug(TAG, "⚠️ onDestroy: Attività distrutta")


        //INVIO STACKTRACE PER MARCO VERDE Google Pixel 9
        // Ottieni lo stacktrace corrente
        //val stackTrace = Throwable().stackTrace.joinToString("\n") { it.toString() }

        // Scrivi lo stacktrace nel file
        //writeStackTraceToFile(stackTrace)

        // Crea un Intent per aprire la MainActivity e passare lo stacktrace
        //val intent = Intent(this, MainActivity::class.java)
        //intent.putExtra("stacktrace", stackTrace)  // Passa lo stacktrace tramite Intent
        //startActivity(intent)  // Avvia la MainActivity

        tts?.stop()
        tts?.shutdown()
        usersListener?.remove()
        usersListener = null
        pilotsListener?.remove()
        pilotsListener = null
        saveOnlineStatus(false) // mette a true lo stato di online
        // Sezione Drone ID Broadcast receiver
        if (isDroneReceiverRegistered) {
            try {
                unregisterReceiver(droneDataReceiver)
                isDroneReceiverRegistered = false
                Log.d("DronePilotApp", "DashboardActivity: DroneDataReceiver deregistrato in onDestroy()")
            } catch (e: IllegalArgumentException) {
                Log.e("DronePilotApp", "DashboardActivity: Receiver non registrato: ${e.message}")
            }
        }

        try {
            // Uso questo al posto di bluetoothReceiver?.stopScanning()
            stopService(Intent(this, BluetoothScanService::class.java))
            //wifiAwareReceiver?.stopSession()
            // Uso questo al posto di  wifiBeaconReceiver?.stopScan()
            stopService(Intent(this, WifiBeaconScanService::class.java))
            //
            Log.d("DronePilotApp", "onDestroy Dashboard: Receiver Drone ID fermati")
        } catch (e: SecurityException) {
            Log.w("DronePilotApp", "onDestroy Dashboard: Permessi insufficienti per fermare i receiver: ${e.message}")
        }

        if (::assistant.isInitialized) {
            assistant.shutdown()
        }

        stopAllCrpcBlinks()
        crpcSector?.remove(); crpcSector = null
        crpcDfMarker?.remove(); crpcDfMarker = null
        logout()

    }

    //
    // ASSISTENTE DI VOLO
    //
    private fun getCurrentLocationAndAskFlightZone(assistant: FlightZoneAssistant) {
        // Se c'è un marker di ricerca attivo, usa quello
        if (searchMarker != null) {
            val pos = searchMarker!!.position

            showVoiceFeedback("📍 Controllo la zona cercata...")
            Handler(Looper.getMainLooper()).postDelayed({
                hideVoiceFeedback()
            }, 3000)

            assistant.askPermissionToFly(pos.latitude, pos.longitude)

            return
        }

        // Altrimenti usa la posizione attuale
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("DronePilotApp", "VoiceCommand: Permessi di localizzazione non concessi")
            showVoiceFeedback("❌ Permessi posizione non concessi")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                showVoiceFeedback("📍 Controllo la tua posizione attuale...")
                Handler(Looper.getMainLooper()).postDelayed({
                    hideVoiceFeedback()
                }, 3000)

                assistant.askPermissionToFly(it.latitude, it.longitude)
            } ?: run {
                Log.e("DronePilotApp", "VoiceCommand: Nessuna posizione disponibile")
                showVoiceFeedback("❌ Posizione non disponibile")
            }
        }
    }






    private fun checkAudioPermissionAndStartListening() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 2)
        } else {
            startListening()
        }
    }

    private fun startListening() {
        startMicAnimation()

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                stopMicAnimation()
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { spokenText ->
                    showVoiceFeedback("📢 Hai detto: \"$spokenText\"")

                    val spokenTextLower = spokenText.lowercase()
                    val giorniDopo = when {
                        spokenTextLower.contains("dopodomani") -> 2
                        spokenTextLower.contains("domani") -> 1
                        spokenTextLower.contains("oggi") -> 0
                        else -> null
                    }

                    val richiedeVolo = spokenTextLower.contains(Regex("può volare|posso volare|si può volare|posso far volare|posso usare"))

                    if (richiedeVolo) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            showVoiceFeedback("📡 Controllo lo spazio aereo...")

                            val assistant = FlightZoneAssistant(this@DashboardActivity) {
                                // Callback quando l'assistente ha finito di parlare
                                giorniDopo?.let { giorno ->
                                    val pos = searchMarker?.position
                                    val fallbackLocation = fusedLocationClient.lastLocation

                                    fun processMeteo(lat: Double, lon: Double) {
                                        OpenWeatherManager.getDailyWeather(lat, lon, giorno) { daily ->
                                            if (daily == null) {
                                                val fallbackText = "❌ Meteo non disponibile."
                                                showVoiceFeedback(fallbackText)
                                                speakText(fallbackText)
                                                return@getDailyWeather
                                            }

                                            val report = buildString {
                                                append("Si prevede: ${daily.condition}. ")
                                                append("Temperatura tra ${daily.tempMin} e ${daily.tempMax} gradi. ")
                                                append("Vento ${daily.windSpeed} km/h, raffiche fino a ${daily.windGust}.")
                                            }

                                            runOnUiThread {
                                                //showVoiceFeedback(report)
                                                //speakText(report)
                                                showMeteoOverlay(report)

                                            }
                                        }
                                    }

                                    if (pos != null) {
                                        processMeteo(pos.latitude, pos.longitude)
                                    } else {
                                        if (ActivityCompat.checkSelfPermission(this@DashboardActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                            fallbackLocation.addOnSuccessListener { location ->
                                                location?.let {
                                                    processMeteo(it.latitude, it.longitude)
                                                } ?: run {
                                                    val err = "❌ Posizione non disponibile"
                                                    showVoiceFeedback(err)
                                                    speakText(err)
                                                }
                                            }
                                        } else {
                                            val err = "❌ Permessi posizione mancanti"
                                            showVoiceFeedback(err)
                                            speakText(err)
                                        }
                                    }
                                }
                            }

                            // Questo chiama la logica esistente di volo (parlante)
                            getCurrentLocationAndAskFlightZone(assistant)

                        }, 1000)

                    } else {
                        Handler(Looper.getMainLooper()).postDelayed({
                            showVoiceFeedback("❌ Comando non riconosciuto")
                            speakText("Comando non riconosciuto")
                            hideVoiceFeedback()
                        }, 2000)
                    }
                }
            }

            override fun onError(error: Int) {
                stopMicAnimation()
                showVoiceFeedback("❌ Errore nel riconoscimento vocale")
                Handler(Looper.getMainLooper()).postDelayed({ hideVoiceFeedback() }, 3000)
            }

            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })

        speechRecognizer.startListening(speechIntent)
    }



    private fun speakText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }



    private fun startMicAnimation() {
        val micButton = findViewById<Button>(R.id.voiceZoneButton)
        micButton.setBackgroundResource(R.drawable.rounded_button_listening) // se vuoi cambiare colore

        val animation = AnimationUtils.loadAnimation(this, R.anim.mic_pulse_in)
        micButton.startAnimation(animation)
    }

    private fun stopMicAnimation() {
        val micButton = findViewById<Button>(R.id.voiceZoneButton)
        micButton.clearAnimation()
        micButton.setBackgroundResource(R.drawable.rounded_button_green)
    }

    private fun showVoiceFeedback(message: String) {
        val feedbackText = findViewById<TextView>(R.id.voiceFeedbackText)
        feedbackText.text = message
        feedbackText.visibility = View.VISIBLE
    }

    private fun hideVoiceFeedback() {
        val feedbackText = findViewById<TextView>(R.id.voiceFeedbackText)
        feedbackText.visibility = View.GONE
    }

    // Testo dell'assistente
    fun showAssistantOverlay(text: String) {
        val overlay = findViewById<FrameLayout>(R.id.assistantOverlay)
        val assistantText = findViewById<TextView>(R.id.assistantText)
        overlay.visibility = View.VISIBLE
        assistantText.text = text
    }

    // Testo del meteo
    private fun showMeteoOverlay(text: String) {
        val overlay = findViewById<FrameLayout>(R.id.meteoOverlay)
        val meteoText = findViewById<TextView>(R.id.meteoText)

        runOnUiThread {
            meteoText.text = text
            overlay.visibility = View.VISIBLE
            speakText(text)
        }
    }



    // Full Screen della Mappa
    private fun toggleFullscreen() {
        val mapCard = findViewById<MaterialCardView>(R.id.mapCard)
        val decorView = window.decorView

        if (!isFullscreen) {
            supportActionBar?.hide()
            decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    )
            // Nascondi elementi UI visibili (aggiungi qui i tuoi)
            //findViewById<View>(R.id.droneField)?.visibility = View.GONE
            findViewById<View>(R.id.droneSpinner)?.visibility = View.GONE
            findViewById<View>(R.id.startFlightButton)?.visibility = View.GONE
            findViewById<View>(R.id.stopFlightButton)?.visibility = View.GONE
            findViewById<View>(R.id.weather_forecast_button)?.visibility = View.GONE
            findViewById<View>(R.id.takeoff_spots_button)?.visibility = View.GONE
            findViewById<View>(R.id.onlineUsersText)?.visibility = View.GONE
            findViewById<View>(R.id.chatUsersText)?.visibility = View.GONE
            findViewById<View>(R.id.driLed)?.visibility = View.GONE
            findViewById<View>(R.id.lowerLimitTextView)?.visibility = View.GONE
            findViewById<View>(R.id.pilotNearAlert)?.visibility = View.GONE
            findViewById<View>(R.id.chatToggle)?.visibility = View.GONE
            findViewById<View>(R.id.chatTitle)?.visibility = View.GONE
            findViewById<View>(R.id.chatLabelOn)?.visibility = View.GONE
            findViewById<View>(R.id.chatLabelOff)?.visibility = View.GONE
            findViewById<View>(R.id.new_message_text)?.visibility = View.GONE
            findViewById<View>(R.id.new_message_icon)?.visibility = View.GONE
            findViewById<View>(R.id.ar_view_button)?.visibility = View.GONE
            // Espandi la mappa
            val heightInDp = 650
            val scale = resources.displayMetrics.density
            mapCard.layoutParams.height = (heightInDp * scale).toInt()
            mapCard.requestLayout()

        } else {
            supportActionBar?.hide()

            // Ripristina altezza 325dp (convertita in pixel)
            val heightInDp = 325
            val scale = resources.displayMetrics.density
            mapCard.layoutParams.height = (heightInDp * scale).toInt()
            mapCard.requestLayout()


            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            //findViewById<View>(R.id.droneField)?.visibility = View.VISIBLE
            findViewById<View>(R.id.droneSpinner)?.visibility = View.VISIBLE
            findViewById<View>(R.id.startFlightButton)?.visibility = View.VISIBLE
            findViewById<View>(R.id.stopFlightButton)?.visibility = View.VISIBLE
            findViewById<View>(R.id.weather_forecast_button)?.visibility = View.VISIBLE
            findViewById<View>(R.id.takeoff_spots_button)?.visibility = View.VISIBLE
            findViewById<View>(R.id.onlineUsersText)?.visibility = View.VISIBLE
            findViewById<View>(R.id.chatUsersText)?.visibility = View.VISIBLE
            findViewById<View>(R.id.driLed)?.visibility = View.VISIBLE
            findViewById<View>(R.id.lowerLimitTextView)?.visibility = View.VISIBLE
            findViewById<View>(R.id.pilotNearAlert)?.visibility = View.VISIBLE
            findViewById<View>(R.id.chatToggle)?.visibility = View.VISIBLE
            findViewById<View>(R.id.chatTitle)?.visibility = View.VISIBLE
            findViewById<View>(R.id.chatLabelOn)?.visibility = View.VISIBLE
            findViewById<View>(R.id.chatLabelOff)?.visibility = View.VISIBLE
            findViewById<View>(R.id.new_message_text)?.visibility = View.VISIBLE
            findViewById<View>(R.id.new_message_icon)?.visibility = View.VISIBLE
            findViewById<View>(R.id.ar_view_button)?.visibility = View.VISIBLE
        }

        isFullscreen = !isFullscreen
    }

    // Ricarica lo spinner dei droni al rientro da Impostazioni
    private fun loadDronesForSpinner() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val dronesRef = Firebase.firestore.collection("pilotProfiles").document(uid).collection("drones")

        dronesRef.get().addOnSuccessListener { result ->
            val droneNames = result.documents.mapNotNull { it.getString("name") }

            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, droneNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            droneSpinner.adapter = adapter

            val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
            val ultimoDrone = prefs.getString("ultimoDrone", null)

            val defaultPosition = droneNames.indexOf(ultimoDrone).takeIf { it >= 0 }
                ?: droneNames.lastIndex.takeIf { droneNames.isNotEmpty() }

            if (defaultPosition != null) {
                droneSpinner.setSelection(defaultPosition)
                selectedDrone = droneNames[defaultPosition]

                // 🔄 Salva comunque il nuovo drone come ultimo selezionato
                prefs.edit().putString("ultimoDrone", selectedDrone).apply()
            } else {
                selectedDrone = null
            }

            droneSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    selectedDrone = droneNames[position]
                    prefs.edit().putString("ultimoDrone", selectedDrone).apply()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedDrone = null
                }
            }
        }
    }


    // Controlla se la Pre Flight Checklist è completa
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHECKLIST_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val safeUserName = userName ?: "Pilota Anonimo"
            val safeDroneName = selectedDrone ?: "Drone Sconosciuto"
            startFlight(safeUserName, safeDroneName)
        }
    }


}
