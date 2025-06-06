package com.kwos.dronepilotapp

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import android.util.Log
import android.widget.ProgressBar


class DroneLogActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fileInfoTextView: TextView
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    private lateinit var summaryModel: TextView
    private lateinit var summaryDuration: TextView
    private lateinit var summaryBattery: TextView
    private lateinit var summaryDistance: TextView
    private lateinit var summaryMaxSpeed: TextView
    private lateinit var summaryMaxAltitude: TextView
    private var lastParsedJson: JSONObject? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drone_log)

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

        summaryModel = findViewById(R.id.summaryModel)
        summaryDuration = findViewById(R.id.summaryDuration)
        summaryBattery = findViewById(R.id.summaryBattery)
        summaryDistance = findViewById(R.id.summaryDistance)
        summaryMaxSpeed = findViewById(R.id.summaryMaxSpeed)
        summaryMaxAltitude = findViewById(R.id.summaryMaxAltitude)

        fileInfoTextView = TextView(this).apply {
            textSize = 14f
            setTextColor(ContextCompat.getColor(this@DroneLogActivity, android.R.color.black))
            setPadding(0, 16, 0, 0)
        }
        findViewById<android.widget.LinearLayout>(R.id.droneLogLayout).addView(fileInfoTextView, 3)

        findViewById<Button>(R.id.importLogButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "text/plain"
            }
            startActivityForResult(intent, 1234)
        }


        // Bottone Istruzioni
        val layout = findViewById<android.widget.LinearLayout>(R.id.droneLogLayout)
        val helpButton = Button(this).apply {
            text = "\u2139\uFE0F Istruzioni"
            setOnClickListener { showImportInstructions() }
        }
        layout.addView(helpButton)

        // Pulsante per chiudere la finestra
        val closeButton: Button = findViewById(R.id.close_dronelog_button)
        closeButton.setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.sendToMapButton).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Conferma pubblicazione volo")
                .setMessage("Il volo del tuo drone sarà visualizzato da tutti sulla mappa")
                .setPositiveButton("OK") { _, _ ->
                    lastParsedJson?.let { saveFlightToFirestore(it) }
                }
                .setNegativeButton("NO", null)
                .show()
        }

        mapView = findViewById(R.id.flightMap)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        val touchInterceptor = findViewById<View>(R.id.touchInterceptor)
        touchInterceptor.setOnTouchListener { _, _ ->
            mapView.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234 && resultCode == Activity.RESULT_OK) {
            data?.data?.also { uri ->
                var fileName = "file selezionato"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex)
                        }
                    }
                }
                fileInfoTextView.text = "📄 Hai selezionato: $fileName"
                uploadLogFile(uri)
            }
        }
    }

    private fun uploadLogFile(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val fileBytes = inputStream.readBytes()

        var fileName = "log.txt"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("logfile", fileName, RequestBody.create("text/plain".toMediaTypeOrNull(), fileBytes))
            .build()

        val request = Request.Builder()
            .url("http://91.121.90.186:5555/upload")
            .addHeader("X-API-KEY", "RaDa0707")
            .post(requestBody)
            .build()

        runOnUiThread {
            fileInfoTextView.text = "📄 Attendere, sto interpretando il file di log…"
        }

        Thread {
            try {
                val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()
                val response = client.newCall(request).execute()
                val bodyString = response.body?.string()

                runOnUiThread {
                    if (response.isSuccessful && bodyString != null) {
                        val json = JSONObject(bodyString)
                        lastParsedJson = json

                        val model = json.optString("model", "-")
                        val totalSeconds = json.optInt("duration", 0)
                        val durationFormatted = String.format("%d min %02d sec", totalSeconds / 60, totalSeconds % 60)

                        val maxSpeed = json.optDouble("maxSpeed", 0.0)
                        val distance = json.optDouble("distanceMeters", 0.0)
                        val maxAltitude = json.optDouble("maxAltitude", 0.0)
                        val batteryStart = json.optInt("batteryStart", -1)
                        val batteryEnd = json.optInt("batteryEnd", -1)

                        summaryModel.text = "🚁 Drone: $model"
                        summaryDuration.text = "⏱️ Durata: ${durationFormatted}"
                        summaryDistance.text = "📏 Distanza totale: %.1f Km".format(distance)
                        summaryMaxSpeed.text = "🚀 Velocità Max: %.1f Km/h".format(maxSpeed)
                        summaryMaxAltitude.text = "🗻 Altezza Max: %.1f m".format(maxAltitude)
                        summaryBattery.text = if (batteryStart >= 0 && batteryEnd >= 0) {
                            "🔋 Batteria: $batteryStart% → $batteryEnd%"
                        } else {
                            "🔋 Batteria: -"
                        }
                        Log.d("DroneLogActivity", "JSON ricevuto: $json")
                        drawTrajectory(json)
                        fileInfoTextView.text = "✅ Log interpretato con successo!"
                    } else {
                        fileInfoTextView.text = "❌ Errore upload: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    fileInfoTextView.text = "❌ Errore rete: ${e.message}"
                }
            }
        }.start()
    }

    private fun saveFlightToFirestore(json: JSONObject) {
        val (dialog, views) = showUploadProgressDialog()
        val (statusText, progressBar) = views

        val serial = json.optString("aircraftSn", null) ?: return
        val model = json.optString("model", "-")
        val trajectory = json.optJSONArray("trajectory") ?: return

        val firestore = FirebaseFirestore.getInstance()
        val trajectoryRef = firestore.collection("trajectories").document(serial).collection("points")

        val total = trajectory.length()
        var uploaded = 0

        fun updateProgress() {
            val percent = (uploaded * 100) / total
            runOnUiThread {
                progressBar.progress = percent
                statusText.text = "📡 Caricamento: $percent%"
            }
        }

        for (i in 0 until total) {
            val point = trajectory.getJSONObject(i)
            val lat = point.optDouble("lat", Double.NaN)
            val lon = point.optDouble("lon", Double.NaN)

            if (!lat.isNaN() && !lon.isNaN()) {
                val data = hashMapOf(
                    "lat" to lat,
                    "lon" to lon,
                    "timestamp" to System.currentTimeMillis()
                )
                trajectoryRef.add(data).addOnCompleteListener {
                    uploaded++
                    updateProgress()

                    if (uploaded == total) {
                        // Ultimo punto, ora invia il documento finale
                        val last = trajectory.getJSONObject(trajectory.length() - 1)
                        val maxAltitude = json.optDouble("maxAltitude", 0.0)
                        val maxSpeedKmH = json.optDouble("maxSpeed", 0.0)
                        val maxSpeedMS = maxSpeedKmH / 3.6
                        val maxSpeedRounded = String.format(Locale.US, "%.1f", maxSpeedMS).toDouble()

                        val droneData = hashMapOf(
                            "lat" to last.optDouble("lat"),
                            "lon" to last.optDouble("lon"),
                            "altitude" to maxAltitude,
                            "speed" to maxSpeedRounded,
                            "model" to model,
                            "timestamp" to System.currentTimeMillis()
                        )
                        firestore.collection("detected_drones").document(serial).set(droneData)
                            .addOnSuccessListener {
                                dialog.dismiss()
                                Toast.makeText(this, "✅ Dati inviati, ora chiudi e vedi la mappa sulla Dashboard", Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener {
                                dialog.dismiss()
                                Toast.makeText(this, "❌ Errore nell'invio dei dati", Toast.LENGTH_LONG).show()
                            }
                    }
                }
            } else {
                uploaded++
                updateProgress()
            }
        }
    }


    private fun drawTrajectory(json: JSONObject) {
        val trajectoryArray = json.optJSONArray("trajectory") ?: return
        val path = mutableListOf<LatLng>()

        Log.d("DroneLogActivity", "Trajectory array: $trajectoryArray, size: ${trajectoryArray.length()}")
        for (i in 0 until trajectoryArray.length()) {
            val point = trajectoryArray.getJSONObject(i)
            val lat = point.optDouble("lat", Double.NaN)
            val lon = point.optDouble("lon", Double.NaN)
            if (!lat.isNaN() && !lon.isNaN()) {
                path.add(LatLng(lat, lon))
            }
        }

        googleMap?.apply {
            clear()
            addPolyline(PolylineOptions().addAll(path).color(ContextCompat.getColor(this@DroneLogActivity, R.color.purple_700)).width(5f))
            if (path.isNotEmpty()) {
                moveCamera(CameraUpdateFactory.newLatLngZoom(path.first(), 16f))
            }
        }
    }

    private fun showUploadProgressDialog(): Pair<AlertDialog, Pair<TextView, ProgressBar>> {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.progress_upload, null)

        val statusText = dialogLayout.findViewById<TextView>(R.id.uploadStatusText)
        val progressBar = dialogLayout.findViewById<ProgressBar>(R.id.uploadProgressBar)

        builder.setView(dialogLayout)
        builder.setCancelable(false)

        val dialog = builder.create()
        dialog.show()

        return Pair(dialog, Pair(statusText, progressBar))
    }


    private fun showImportInstructions() {
        val message = """
            📥 Come importare un file di log DJI:

            1️⃣ Collega lo smartphone al PC con un cavo USB
            2️⃣ Sul PC, apri la cartella:
            Questo PC > realme 11 Pro+ 5G > Memoria condivisa interna > Android > data > dji.go.v5 > files > FlightRecord
            3️⃣ Copia il file di log (es. DJIFlightRecord_2025-05-15_[12-05-38].txt)
            4️⃣ Incollalo in una cartella accessibile, come Download
            5️⃣ Torna nell'app Drone Pilot e premi "Importa log"
            6️⃣ Seleziona il file .txt dalla cartella Download
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Istruzioni per importare un log")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
