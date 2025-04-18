package com.kwos.dronepilotapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.firestore.FieldValue
//per il padding
import androidx.core.view.ViewCompat
import android.view.View
import android.widget.EditText
import com.google.android.gms.maps.model.Marker


class TakeoffSpotsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    private var selectedMarker: Marker? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_takeoffspots)

        //Fa il padding automatico (non va a coprire i tasti funzione per i
        //telefoni con immersive view
        // Recupera la root view del layout
        val rootView = findViewById<View>(android.R.id.content)

        // Applica il padding per evitare che gli elementi vengano coperti
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.updatePadding(
                top = systemBars.top, // Evita sovrapposizione con la status bar
                bottom = systemBars.bottom // Evita sovrapposizione con la navigation bar
            )

            WindowInsetsCompat.CONSUMED
        }
        // fine padding


        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        // Setup mappa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map_fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Init location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Pulsanti
        findViewById<Button>(R.id.btn_add_spot).setOnClickListener {
            addSpot()
        }

        findViewById<Button>(R.id.btn_remove_spot).setOnClickListener {
            removeSpot()
        }

        // Pulsante per chiudere la finestra
        findViewById<Button>(R.id.close_spot_button).setOnClickListener {
            finish()
        }

    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.setOnMarkerClickListener { marker ->
            selectedMarker = marker
            marker.showInfoWindow()
            true
        }

        map.setOnInfoWindowClickListener { marker ->
            val lat = marker.position.latitude
            val lng = marker.position.longitude
            val uri = Uri.parse("https://www.google.com/maps?q=$lat,$lng")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps") // apre direttamente Google Maps se installato
            startActivity(intent)
        }

        loadTakeoffSpots()
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 7f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun loadTakeoffSpots() {
        val db = FirebaseFirestore.getInstance()
        val iconUrl = "https://www.kwos.org/appoggio/droni/dronepilotapp/icons8-drone-takeoff-96.png"

        db.collection("takeoff_spots")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val lat = document.getDouble("lat") ?: continue
                    val lng = document.getDouble("lng") ?: continue
                    val count = document.getLong("count") ?: 1
                    val descrizione = document.getString("descrizione") ?: "Nessuna descrizione"

                    val position = LatLng(lat, lng)
                    val title = "Spot segnalato $count volt${if (count > 1) "e" else "a"}"

                    Glide.with(this)
                        .asBitmap()
                        .load(iconUrl)
                        .into(object : CustomTarget<Bitmap>(64, 64) {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                val icon = BitmapDescriptorFactory.fromBitmap(resource)
                                map.addMarker(
                                    MarkerOptions()
                                        .position(position)
                                        .title(title)
                                        .snippet(descrizione)
                                        .icon(icon)
                                )
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Errore nel caricamento degli spot: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun addSpot() {

        val descriptionEditText = findViewById<EditText>(R.id.edit_spot_description)
        val spotDescription = descriptionEditText.text.toString().trim()
        // Verifica se il permesso di accesso alla posizione è concesso
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permesso concesso, ottieni la posizione
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val db = FirebaseFirestore.getInstance()
                        val newSpotLat = location.latitude
                        val newSpotLng = location.longitude

                        val newLocation = Location("").apply {
                            latitude = newSpotLat
                            longitude = newSpotLng
                        }

                        db.collection("takeoff_spots")
                            .get()
                            .addOnSuccessListener { result ->
                                var nearbySpotId: String? = null

                                for (document in result) {
                                    val lat = document.getDouble("lat") ?: continue
                                    val lng = document.getDouble("lng") ?: continue
                                    val locationDb = Location("").apply {
                                        latitude = lat
                                        longitude = lng
                                    }

                                    if (newLocation.distanceTo(locationDb) <= 500) {
                                        nearbySpotId = document.id
                                        break
                                    }
                                }

                                if (nearbySpotId != null) {
                                    // Incrementa il count
                                    val spotRef = db.collection("takeoff_spots").document(nearbySpotId)
                                    db.runTransaction { transaction ->
                                        val snapshot = transaction.get(spotRef)
                                        val currentCount = snapshot.getLong("count") ?: 1
                                        transaction.update(spotRef, "count", currentCount + 1)
                                    }.addOnSuccessListener {
                                        Toast.makeText(this, "Spot già esistente, segnalazione aggiunta!", Toast.LENGTH_SHORT).show()
                                        loadTakeoffSpots()
                                    }
                                } else {
                                    // Nuovo spot
                                    val newSpot = hashMapOf(
                                        "lat" to newSpotLat,
                                        "lng" to newSpotLng,
                                        "count" to 1,
                                        "userId" to userId,
                                        "timestamp" to FieldValue.serverTimestamp(),
                                        "descrizione" to spotDescription
                                    )
                                    db.collection("takeoff_spots")
                                        .add(newSpot)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Nuovo spot aggiunto!", Toast.LENGTH_SHORT).show()
                                            loadTakeoffSpots()
                                        }
                                }
                            }
                    } else {
                        Toast.makeText(this, "Posizione non disponibile", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Errore nell'ottenere la posizione", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Permesso non concesso, richiedi il permesso
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun removeSpot() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        if (selectedMarker != null) {
            // 🔸 Se un marker è stato selezionato manualmente
            val marker = selectedMarker!!
            val lat = marker.position.latitude
            val lng = marker.position.longitude

            db.collection("takeoff_spots")
                .whereEqualTo("lat", lat)
                .whereEqualTo("lng", lng)
                .get()
                .addOnSuccessListener { result ->
                    var spotToRemoveId: String? = null
                    for (document in result) {
                        val docUserId = document.getString("userId")
                        val count = document.getLong("count") ?: 1

                        if (docUserId == userId && count == 1L) {
                            spotToRemoveId = document.id
                            break
                        }
                    }

                    if (spotToRemoveId != null) {
                        db.collection("takeoff_spots").document(spotToRemoveId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this, "Spot rimosso!", Toast.LENGTH_SHORT).show()
                                marker.remove()
                                selectedMarker = null
                                loadTakeoffSpots()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Errore nella rimozione dello spot", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Non puoi rimuovere questo spot", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // 🔸 Nessun marker selezionato, usa la logica basata sulla posizione GPS
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val newSpotLat = location.latitude
                            val newSpotLng = location.longitude

                            val newLocation = Location("").apply {
                                latitude = newSpotLat
                                longitude = newSpotLng
                            }

                            db.collection("takeoff_spots")
                                .get()
                                .addOnSuccessListener { result ->
                                    var spotToRemoveId: String? = null

                                    for (document in result) {
                                        val lat = document.getDouble("lat") ?: continue
                                        val lng = document.getDouble("lng") ?: continue
                                        val locationDb = Location("").apply {
                                            latitude = lat
                                            longitude = lng
                                        }

                                        if (newLocation.distanceTo(locationDb) <= 500) {
                                            val storedUserId = document.getString("userId")
                                            val count = document.getLong("count") ?: 1

                                            if (storedUserId == userId && count == 1L) {
                                                spotToRemoveId = document.id
                                                break
                                            }
                                        }
                                    }

                                    if (spotToRemoveId != null) {
                                        db.collection("takeoff_spots")
                                            .document(spotToRemoveId)
                                            .delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(this, "Spot rimosso! Si cancella quando esci", Toast.LENGTH_SHORT).show()
                                                loadTakeoffSpots()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this, "Errore nella rimozione dello spot", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Toast.makeText(this, "Nessun spot valido trovato per la rimozione", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Posizione non disponibile", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Errore nell'ottenere la posizione", Toast.LENGTH_SHORT).show()
                    }
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    // Gestisci la risposta del permesso
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                // Se il permesso è stato concesso
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Ora possiamo ottenere la posizione
                    addSpot()
                } else {
                    // Il permesso è stato negato
                    Toast.makeText(this, "Permesso di accesso alla posizione negato", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
