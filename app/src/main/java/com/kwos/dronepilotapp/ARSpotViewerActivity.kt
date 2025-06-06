package com.kwos.dronepilotapp

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.camera.view.PreviewView
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.android.volley.Request
import java.net.URLEncoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.FusedLocationProviderClient
import kotlin.math.*
import android.opengl.Matrix
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.maps.model.LatLng
import com.kwos.dronepilotapp.AROverlayView
import com.kwos.dronepilotapp.ZoneColorUtils.distanceBetweenMeters
import java.io.IOException
import okhttp3.Call
import com.google.firebase.firestore.FirebaseFirestore


class ARSpotViewerActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: AROverlayView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var gravity = FloatArray(3)
    private var geomagnetic = FloatArray(3)

    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private val poiList = mutableListOf<POI>()
    private lateinit var radiusSeekBar: SeekBar
    private lateinit var azimuthOffsetSeekBar: SeekBar
    private lateinit var radiusLabel: TextView

    private var azimuthOffset: Float = 0f

    private val PREFS_NAME = "ARPrefs"
    private val PREF_KEY_OFFSET = "azimuth_offset"

    private var nearbyZones10km: List<NoFlyZone> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.activity_ar_spot_viewer)
        radiusSeekBar = findViewById(R.id.radiusSeekBar)
        azimuthOffsetSeekBar = findViewById(R.id.azimuthOffsetSeekBar)
        val offsetLabel = findViewById<TextView>(R.id.offsetLabel)
        radiusLabel = findViewById(R.id.radiusLabel)


        previewView = findViewById(R.id.camera_preview)
        overlayView = findViewById(R.id.overlay_view)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

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
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false // o false, dipende dal tema

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Listener SeekBar per la distanza
        val initialRadius = 500.0 + (radiusSeekBar.progress * 500.0)
        radiusLabel.text = "Raggio: ${initialRadius.toInt()} m"

        radiusSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val selectedRadius = 500.0 + (progress * 500.0)
                radiusLabel.text = "Raggio: ${selectedRadius.toInt()} m"  // 🔥 aggiorna label
                Log.d("ARSpotViewer", "🎚️ Radius cambiato manualmente: $selectedRadius m")

                // Ricalcolo dei POI con il nuovo raggio
                fetchTouristicPOIs(
                    currentLat,
                    currentLon,
                    radiusMeters = selectedRadius.toInt(),
                    onResult = { pois ->
                        poiList.clear()
                        poiList.addAll(pois)

                        val filtered = filterClosestPOIs(
                            pois,
                            currentLat,
                            currentLon,
                            minCount = 5,
                            startRadius = selectedRadius,
                            maxRadius = selectedRadius
                        )

                        Log.d("ARSpotViewer", "📍 POI filtrati: ${filtered.size}")
                        filtered.forEach { Log.d("ARSpotViewer", "→ ${it.name}") }

                        val userLatLng = LatLng(currentLat, currentLon)
                        overlayView.setPOIs(filtered, userLatLng)
                        overlayView.setMaxVisibleDistance(selectedRadius)

                        // 🛡️ Filtra e aggiorna anche le zone No-Fly
                        val zoneFiltered = nearbyZones10km.filter {
                            distanceBetweenMeters(userLatLng, it.center) < selectedRadius
                        }
                        Log.d("ARSpotViewer", "🛡️ Zone nel raggio $selectedRadius m: ${zoneFiltered.size}")
                        overlayView.setNoFlyZones(zoneFiltered)
                        overlayView.invalidate()

                        loadNearbyPilots(currentLat, currentLon, selectedRadius)

                    },
                    onError = { msg -> Log.e("ARSpotViewer", msg) }
                )
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Seeker per offset di azimuth
        azimuthOffsetSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val offset = progress - 180
                overlayView.setAzimuthOffset(offset.toFloat())
                offsetLabel.text = "Offset: ${offset}°" // 🔥 aggiorna label
                Log.d("ARSpotViewer", "🧭 Offset azimuth settato: $offset°")

                // Salva nelle preferenze
                val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                prefs.edit().putInt(PREF_KEY_OFFSET, progress).apply()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedOffset = prefs.getInt(PREF_KEY_OFFSET, 180)
        azimuthOffsetSeekBar.progress = savedOffset

        val initialOffset = azimuthOffsetSeekBar.progress - 180
        offsetLabel.text = "Offset: ${initialOffset}°"


        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLat = it.latitude
                currentLon = it.longitude

                Log.d("ARSpotViewer", "📍 Posizione ottenuta: ${it.latitude}, ${it.longitude}")

                val progress = radiusSeekBar.progress
                val selectedRadius = 500.0 + (progress * 500.0) // da 500m a 10500m
                Log.d("ARSpotViewer", "🎚️ Raggio selezionato: $selectedRadius m")

                loadNearbyPilots(currentLat, currentLon, selectedRadius)

                fetchTouristicPOIs(
                    currentLat,
                    currentLon,
                    radiusMeters = selectedRadius.toInt(),
                    onResult = { pois ->
                        poiList.clear()
                        poiList.addAll(pois)

                        val filtered = filterClosestPOIs(
                            pois,
                            currentLat,
                            currentLon,
                            minCount = 5,
                            startRadius = selectedRadius,
                            maxRadius = selectedRadius
                        )

                        Log.d("ARSpotViewer", "📍 POI filtrati: ${filtered.size}")
                        filtered.forEach { Log.d("ARSpotViewer", "→ ${it.name}") }

                        val userLatLng = LatLng(currentLat, currentLon)
                        overlayView.setPOIs(filtered, userLatLng)

                        val url = "https://www.kwos.org/appoggio/droni/dflight_geozones.json"
                        val request = okhttp3.Request.Builder().url(url).build()
                        val client = okhttp3.OkHttpClient()

                        client.newCall(request).enqueue(object : okhttp3.Callback {
                            override fun onFailure(call: okhttp3.Call, e: IOException) {
                                Log.e("ARSpotViewer", "❌ Errore caricamento zone: ${e.message}")
                            }

                            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                                response.body?.string()?.let { jsonString ->
                                    val allZones = ZoneColorUtils.parseZonesFromJson(jsonString)
                                    Log.d("ARSpotViewer", "✅ Zone parse trovate: ${allZones.size}")

                                    val userLatLng = LatLng(currentLat, currentLon)
                                    nearbyZones10km = allZones.filter {
                                        distanceBetweenMeters(userLatLng, it.center) < 10000  // 10 km
                                    }

                                    Log.d("ARSpotViewer", "📦 Zone nel raggio 10 km: ${nearbyZones10km.size}")
                                    nearbyZones10km.forEach { zone ->
                                        Log.d("ARSpotViewer", "→ ${zone.name} @ ${zone.center} - alt min ${zone.lowerLimit} m")
                                    }

                                    // Mostra subito le zone nel raggio iniziale selezionato
                                    val filteredZones = nearbyZones10km.filter {
                                        distanceBetweenMeters(userLatLng, it.center) < selectedRadius
                                    }

                                    runOnUiThread {
                                        overlayView.setNoFlyZones(filteredZones)
                                        overlayView.invalidate()
                                        Log.d("ARSpotViewer", "📡 Zone settate sull’overlay (iniziale) e invalidate.")
                                    }
                                }
                            }



                        })

                        overlayView.setMaxVisibleDistance(selectedRadius)

                    },
                    onError = { msg -> Log.e("ARSpotViewer", msg) }
                )
            } ?: Log.e("ARSpotViewer", "❌ Nessuna posizione disponibile.")
        }


    }

    private fun filterClosestPOIs(
        allPOIs: List<POI>,
        userLat: Double,
        userLon: Double,
        minCount: Int = 5,
        startRadius: Double = 500.0,
        maxRadius: Double = 10000.0
    ): List<POI> {
        var radius = startRadius
        var filtered: List<POI>

        do {
            filtered = allPOIs.filter {
                haversine(userLat, userLon, it.latitude, it.longitude) <= radius
            }
            radius += 500
        } while (filtered.size < minCount && radius <= maxRadius)

        return filtered
    }


    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // raggio medio terrestre in metri
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startCamera() else finish()
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                Log.e("ARSpotViewer", "Errore camera: ${exc.message}", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    fun fetchTouristicPOIs(lat: Double, lon: Double, radiusMeters: Int = 3000,
                           onResult: (List<POI>) -> Unit, onError: (String) -> Unit) {
        val query = """
            [out:json];
            (
              node["tourism"="attraction"](around:$radiusMeters, $lat, $lon);
              node["historic"](around:$radiusMeters, $lat, $lon);
              node["natural"="viewpoint"](around:$radiusMeters, $lat, $lon);
              node["place"~"village|town|hamlet"](around:$radiusMeters, $lat, $lon);
            );
            out;
        """.trimIndent()

        val url = "https://overpass-api.de/api/interpreter?data=${URLEncoder.encode(query, "UTF-8")}"
        val requestQueue = Volley.newRequestQueue(this)

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val poiList = mutableListOf<POI>()
                val elements = response.optJSONArray("elements") ?: return@JsonObjectRequest
                for (i in 0 until elements.length()) {
                    val elem = elements.getJSONObject(i)
                    val tags = elem.optJSONObject("tags") ?: continue
                    val name = tags.optString("name", "").trim()
                    if (name.isEmpty()) continue

                    val type = when {
                        tags.has("place") -> tags.getString("place")
                        tags.has("historic") -> "historic"
                        tags.has("tourism") -> tags.getString("tourism")
                        tags.has("natural") -> tags.getString("natural")
                        else -> "poi"
                    }

                    val latPoi = elem.optDouble("lat", 0.0)
                    val lonPoi = elem.optDouble("lon", 0.0)
                    poiList.add(POI(name, latPoi, lonPoi, type))
                }
                onResult(poiList)
            },
            { error ->
                val errorBody = error.networkResponse?.statusCode?.toString() ?: "N/A"
                Log.e("ARSpotViewer", "❌ Errore richiesta POI - HTTP: $errorBody, msg: ${error.message}")
                onError("Errore richiesta POI: ${error.message}")
            }

        )

        requestQueue.add(jsonObjectRequest)
    }


    override fun onResume() {
        super.onResume()

        val rotVec = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (rotVec != null) {
            sensorManager.registerListener(this, rotVec, SensorManager.SENSOR_DELAY_UI)
            Log.d("ARSpotViewer", "✅ Rotation vector registrato")
        } else {
            Log.e("ARSpotViewer", "❌ Rotation vector non disponibile")
        }

        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
            Log.d("ARSpotViewer", "✅ Accelerometro registrato")
        }

        if (magnet != null) {
            sensorManager.registerListener(this, magnet, SensorManager.SENSOR_DELAY_UI)
            Log.d("ARSpotViewer", "✅ Magnetometro registrato")
        }
    }


    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)

                val azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

                overlayView.setOrientation(azimuth, pitch, roll)
                //Log.d("ARSpotViewer", "🧭 Orientamento: azimuth=$azimuth°, pitch=$pitch°, roll=$roll°")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}



    private fun loadNearbyPilots(userLat: Double, userLon: Double, maxDistanceMeters: Double) {
        val db = FirebaseFirestore.getInstance()
        val pilotsRef = db.collection("piloti")

        pilotsRef.get().addOnSuccessListener { snapshot ->
            val pilotList = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("name") ?: return@mapNotNull null
                val drone = doc.getString("drone") ?: "drone"
                val lat = doc.getDouble("latitude") ?: return@mapNotNull null
                val lon = doc.getDouble("longitude") ?: return@mapNotNull null

                val distance = haversine(userLat, userLon, lat, lon)
                if (distance <= maxDistanceMeters) {
                    RemotePilot(name, drone, lat, lon)
                } else null
            }

            Log.d("ARSpotViewer", "👨‍✈️ Piloti caricati e filtrati: ${pilotList.size}")
            runOnUiThread {
                overlayView.setRemotePilots(pilotList)
            }
        }.addOnFailureListener {
            Log.e("ARSpotViewer", "❌ Errore caricamento piloti: ${it.message}")
        }
    }

}
