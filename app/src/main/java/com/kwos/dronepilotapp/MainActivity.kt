package com.kwos.dronepilotapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class MainActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.hide()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val fullNameField = findViewById<EditText>(R.id.fullNameField)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            googleApiAvailability.makeGooglePlayServicesAvailable(this)
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            loginUser(email, password)
        }

        registerButton.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            val fullName = fullNameField.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && fullName.isNotEmpty()) {
                registerUser(email, password, fullName)
            } else {
                Toast.makeText(this, "Compila tutti i campi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login riuscito!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Errore: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun registerUser(email: String, password: String, fullName: String) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val userId = auth.currentUser?.uid
                val userMap = hashMapOf(
                    "uid" to userId,
                    "email" to email,
                    "fullName" to fullName
                )

                userId?.let {
                    db.collection("users").document(it).set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registrazione completata!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Register", "Errore durante il salvataggio", e)
                        }
                }
            } else {
                Log.e("Register", "Registrazione fallita", task.exception)
            }
        }
    }
}

class DashboardActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var mapFragment: SupportMapFragment? = null
    private var userName: String? = null  // Ora viene caricato da loadUserName()
    private var droneName: String? = null
    private lateinit var locationCallback: LocationCallback  // Variabile per il callback della posizione

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        loadUserName() // Carica il nome del pilota all'avvio

        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val startFlightButton = findViewById<Button>(R.id.startFlightButton)
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            mMap.isMyLocationEnabled = true
            loadPilots()
        }
    }

    private fun loadPilots() {
        db.collection("piloti").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val lat = document.get("latitude")?.toString()?.toDoubleOrNull()
                val lng = document.get("longitude")?.toString()?.toDoubleOrNull()
                val name = document.getString("name") ?: "Sconosciuto"
                val drone = document.getString("drone") ?: "N/D"
                if (lat != null && lng != null) {
                    val position = LatLng(lat, lng)
                    mMap.addMarker(MarkerOptions().position(position).title("$name - $drone"))
                }
            }
        }
    }

    private fun startFlight(userName: String, droneName: String) {
        if (!::mMap.isInitialized) {
            Toast.makeText(this, "La mappa non è pronta", Toast.LENGTH_SHORT).show()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15f))

                startLocationUpdates(userId, userName, droneName)
            }
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
                        "drone" to droneName
                    )
                    db.collection("piloti").document(userId).set(position)

                    val userPosition = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(userPosition).title("$userName - $droneName"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userPosition, 15f))
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
}


