package com.kwos.dronepilotapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
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
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.card.MaterialCardView

class SpotDetailsActivity : AppCompatActivity() {

    private lateinit var spotPhotoCard: MaterialCardView // MaterialCardView per la card
    private lateinit var spotPhoto: ImageView           // ImageView per l'immagine
    private lateinit var spotTitle: TextView
    private lateinit var spotDescription: TextView
    private lateinit var shareButton: Button
    private lateinit var navigateButton: Button

    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var spotName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spot_details)

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

        // Inizializzazione delle viste
        //spotPhotoCard = findViewById(R.id.spot_photo_card)
        spotPhoto = findViewById(R.id.spot_photo)
        spotTitle = findViewById(R.id.spot_title)
        spotDescription = findViewById(R.id.spot_description)
        shareButton = findViewById(R.id.share_button)
        navigateButton = findViewById(R.id.navigate_button)

        // Recupera i dati passati tramite l'intent
        val photoUrl = intent.getStringExtra("photoUrl") ?: ""
        spotName = intent.getStringExtra("name") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        lat = intent.getDoubleExtra("lat", 0.0)
        lng = intent.getDoubleExtra("lng", 0.0)

        // Imposta i dati nel layout
        spotTitle.text = spotName
        spotDescription.text = description

        if (photoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .into(spotPhoto)  // Carica l'immagine nell'ImageView
        } else {
            spotPhoto.visibility = View.GONE  // Nasconde l'immagine se non c'è URL
        }

        // Setup mappa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mini_map_spot) as SupportMapFragment
        mapFragment.getMapAsync { googleMap ->
            val spotLatLng = LatLng(lat, lng)
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(spotLatLng, 16f))

            googleMap.uiSettings.isScrollGesturesEnabled = false
            googleMap.uiSettings.isZoomGesturesEnabled = false
            googleMap.uiSettings.isTiltGesturesEnabled = false
            googleMap.uiSettings.isRotateGesturesEnabled = false
            googleMap.uiSettings.isMapToolbarEnabled = false
            googleMap.uiSettings.isCompassEnabled = false

            val iconUrl = "https://www.kwos.org/appoggio/droni/dronepilotapp/icons8-drone-takeoff-96.png"
            Glide.with(this)
                .asBitmap()
                .load(iconUrl)
                .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(object : com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>(64, 64) {
                    override fun onResourceReady(resource: android.graphics.Bitmap, transition: com.bumptech.glide.request.transition.Transition<in android.graphics.Bitmap>?) {
                        val icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(resource)
                        googleMap.addMarker(MarkerOptions().position(spotLatLng).title(spotName).icon(icon))
                    }

                    override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                })
        }

        // Bottone Share
        shareButton.setOnClickListener {
            shareSpot()
        }

        // Bottone Navigate
        navigateButton.setOnClickListener {
            navigateToSpot()
        }

        // Bottone Chiudi
        findViewById<Button>(R.id.close_spotdetails_button).setOnClickListener {
            finish()
        }
    }

    private fun shareSpot() {
        val shareText = "Spot di volo: $spotName\nPosizione: https://maps.google.com/?q=$lat,$lng"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        startActivity(Intent.createChooser(intent, "Condividi Spot tramite"))
    }

    private fun navigateToSpot() {
        val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Google Maps non installato", Toast.LENGTH_SHORT).show()
        }
    }
}
