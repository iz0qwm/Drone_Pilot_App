package com.kwos.dronepilotapp

import android.Manifest
import android.content.Context
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
import android.view.LayoutInflater
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.firestore.FieldValue
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm

//per il padding
import androidx.core.view.ViewCompat
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.google.android.gms.maps.model.Marker


class TakeoffSpotsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location
    private lateinit var clusterManager: ClusterManager<TakeoffSpotItem>
    private var selectedMarker: Marker? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 1000
    private val REQUEST_CODE_ADD_SPOT = 1001



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
            val intent = Intent(this, AddTakeoffSpotActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE_ADD_SPOT)
            overridePendingTransition(R.anim.slide_in_up, R.anim.slide_out_down)
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

        // Inizializza il ClusterManager
        clusterManager = ClusterManager(this, map)
        clusterManager.renderer = TakeoffSpotRenderer(this, map, clusterManager)
        clusterManager.algorithm = com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm<TakeoffSpotItem>().apply {
            setMaxDistanceBetweenClusteredItems(150) // 🎯 distanza più sensata
        }

        map.setOnCameraIdleListener(clusterManager)
        map.setOnMarkerClickListener(clusterManager)

        // Gestisci il click sui singoli spot
        clusterManager.setOnClusterItemClickListener { item ->
            val intent = Intent(this, SpotDetailsActivity::class.java).apply {
                putExtra("name", item.title)
                putExtra("description", item.snippet)
                putExtra("photoUrl", item.photoUrl)
                putExtra("lat", item.position.latitude)
                putExtra("lng", item.position.longitude)
            }
            startActivity(intent)
            true // Indica che abbiamo gestito il click
        }

        clusterManager.setOnClusterClickListener { cluster ->
            val boundsBuilder = com.google.android.gms.maps.model.LatLngBounds.Builder()
            for (item in cluster.items) {
                boundsBuilder.include(item.position)
            }
            val bounds = boundsBuilder.build()

            try {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngBounds(bounds, 100)
                )
            } catch (e: Exception) {
                // Se qualcosa va male nel calcolo dei bounds (tipo cluster troppo piccolo)
                map.animateCamera(CameraUpdateFactory.zoomIn())
            }

            true // Indica che il click sul cluster è stato gestito
        }

        // Gestione InfoWindow se vuoi personalizzarla (opzionale)
        map.setInfoWindowAdapter(CustomInfoWindowAdapter(this))

        // Carica gli spot
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

        // Prima svuota il clusterManager se hai già elementi caricati
        clusterManager.clearItems()

        db.collection("takeoff_spots")
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    // Prima scarichiamo l'icona del drone
                    Glide.with(this)
                        .asBitmap()
                        .load(iconUrl)
                        .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .into(object : CustomTarget<Bitmap>(64, 64) {
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                val icon = BitmapDescriptorFactory.fromBitmap(resource)

                                for (document in result) {
                                    val lat = document.getDouble("lat") ?: continue
                                    val lng = document.getDouble("lng") ?: continue
                                    val count = document.getLong("count") ?: 1
                                    val name = document.getString("name") ?: "Senza nome"
                                    val description = document.getString("description") ?: "Nessuna descrizione"
                                    val photoUrl = document.getString("photoUrl") ?: ""

                                    val position = LatLng(lat, lng)

                                    //val snippetText = "Segnalato $count volt${if (count > 1) "e" else "a"}\nClicca per informazioni"
                                    val snippetText = "$description\nSegnalato $count volt${if (count > 1) "e" else "a"}"

                                    val item = TakeoffSpotItem(
                                        lat = position.latitude,
                                        lng = position.longitude,
                                        title = name,
                                        snippet = snippetText,
                                        iconBitmap = icon,
                                        photoUrl = photoUrl
                                    )

                                    clusterManager.addItem(item)
                                }

                                clusterManager.cluster() // 🚀 Importantissimo: disegna i marker raggruppati
                                Toast.makeText(this@TakeoffSpotsActivity, "Mappa aggiornata!", Toast.LENGTH_SHORT).show()
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })
                } else {
                    Toast.makeText(this, "Nessuno spot trovato.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Errore nel caricamento degli spot: ${exception.message}", Toast.LENGTH_SHORT).show()
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
                    var photoUrlToDelete: String? = null

                    for (document in result) {
                        val docUserId = document.getString("userId")
                        val count = document.getLong("count") ?: 1

                        if (docUserId == userId && count == 1L) {
                            spotToRemoveId = document.id
                            photoUrlToDelete = document.getString("photoUrl")
                            break
                        }
                    }

                    if (spotToRemoveId != null && photoUrlToDelete != null) {
                        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                        val photoRef = storageRef.storage.getReferenceFromUrl(photoUrlToDelete)

                        // Prima elimina la foto
                        photoRef.delete()
                            .addOnSuccessListener {
                                // Poi elimina il documento Firestore
                                db.collection("takeoff_spots").document(spotToRemoveId!!)
                                    .delete()
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Spot e foto rimossi!", Toast.LENGTH_SHORT).show()
                                        marker.remove()
                                        selectedMarker = null
                                        loadTakeoffSpots()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Errore nella rimozione dello spot", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                Toast.makeText(this, "Errore nella rimozione della foto", Toast.LENGTH_SHORT).show()
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
                                    var photoUrlToDelete: String? = null

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
                                                photoUrlToDelete = document.getString("photoUrl")
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_ADD_SPOT && resultCode == RESULT_OK) {
            // 🔥 Ricarica gli spot
            loadTakeoffSpots()
        }
    }

    class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {

        private lateinit var window: View

        constructor(context: Context) {
            window = LayoutInflater.from(context).inflate(R.layout.custom_info_window, null)
        }

        private fun render(marker: Marker, view: View) {
            val title = marker.title
            val snippet = marker.snippet

            val titleView = view.findViewById<TextView>(R.id.info_window_title)
            val snippetView = view.findViewById<TextView>(R.id.info_window_snippet)

            titleView.text = title
            snippetView.text = snippet
        }

        override fun getInfoWindow(marker: Marker): View? {
            render(marker, window)
            return window
        }

        override fun getInfoContents(marker: Marker): View? {
            render(marker, window)
            return window
        }
    }

}
