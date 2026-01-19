package com.kwos.dronepilotapp

//per il padding

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.Manifest
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class WeatherGpsActivity : AppCompatActivity() {

    private lateinit var gpsStatusHelper: GpsStatusHelper
    private lateinit var tecValueTextView: TextView
    private lateinit var tecStatusTextView: TextView
    private lateinit var tecTitleTextView: TextView

    private val REQUEST_CODE = 1001

    private lateinit var gpsAccuracyText: TextView
    private lateinit var gnssSummaryText: TextView
    private lateinit var fixHistoryText: TextView
    private var lastAccuracy: Float = -1f
    private val fixHistory = mutableListOf<String>()
    private var locationReceived = false
    private val locationTimeoutHandler = Handler(Looper.getMainLooper())
    private lateinit var wifiScanResultTextView: TextView
    private lateinit var wifiChart: LineChart
    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null
    private var magneticListener: SensorEventListener? = null
    private lateinit var magneticChart: LineChart
    private val magneticValues = mutableListOf<Entry>()
    private var timeIndex = 0f


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Abilita l'invio dei crash su Firebase console
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        setContentView(R.layout.activity_weather_gps)

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

        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()
        // FINE EDGE-TO-EDGE


        val closeButton: Button = findViewById(R.id.close_gps_button)

        val gpsalertTextView: TextView = findViewById(R.id.gpsalertTextView)
        tecValueTextView = findViewById(R.id.tecValueTextView)
        tecStatusTextView = findViewById(R.id.tecStatusTextView)
        tecTitleTextView = findViewById(R.id.tecTitleTextView)
        gpsAccuracyText = findViewById(R.id.gps_accuracy_text)
        gnssSummaryText = findViewById(R.id.gnss_summary)
        fixHistoryText = findViewById(R.id.fix_history)
        wifiScanResultTextView = findViewById(R.id.wifiScanResultTextView)
        wifiChart = findViewById(R.id.wifiChart)

        // Magnetometro
        magneticChart = findViewById(R.id.magneticChart)
        setupMagneticChart()

        val magneticFieldValueTextView: TextView = findViewById(R.id.magneticFieldValueTextView)
        val magneticFieldStatusTextView: TextView = findViewById(R.id.magneticFieldStatusTextView)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        magneticListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)

                magneticFieldValueTextView.text = "Campo magnetico: %.1f µT".format(magnitude)

                val (statusText, color) = when {
                    magnitude < 35 -> "⚠️ Valore troppo basso (anomalo)" to Color.GRAY
                    magnitude < 60 -> "🟢 Campo regolare" to Color.rgb(19, 117, 13)
                    magnitude < 100 -> "🟠 Attenzione: possibili interferenze" to Color.rgb(255, 165, 0)
                    else -> "🔴 Disturbo magnetico elevato!" to Color.RED
                }

                // Salva il punto per il grafico
                magneticValues.add(Entry(timeIndex, magnitude))
                if (magneticValues.size > 60) magneticValues.removeAt(0)  // mantieni ultimi 60 punti
                timeIndex += 1f

                val dataSet = LineDataSet(magneticValues, "Campo magnetico (µT)").apply {
                    setColor(ColorTemplate.COLORFUL_COLORS[1])  // 👈 usa setColor al posto di color =
                    setDrawCircles(false)
                    lineWidth = 2f
                    setDrawValues(false)
                }


                magneticChart.data = LineData(dataSet)
                magneticChart.invalidate()

                magneticFieldStatusTextView.text = statusText
                magneticFieldStatusTextView.setTextColor(color)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }



        // Recupera i dati TEC
        MeteoManager.getTecData { tecMean ->
            Handler(Looper.getMainLooper()).postDelayed({
                // Assicurati che l'aggiornamento avvenga nel thread principale
                runOnUiThread {
                    if (tecMean != null) {
                        tecValueTextView.text = "TEC (Total Electron Content): $tecMean TECu"

                        // Cambia il colore e il messaggio in base al valore di tecMean
                        val statusText: String
                        val statusColor: Int

                        when {
                            tecMean < 50 -> {
                                statusText = "CONDIZIONI DI CALMA"
                                statusColor = Color.rgb(19, 117, 13) // VERDE SCURO
                            }
                            tecMean >= 50 && tecMean < 80 -> {
                                statusText = "ATTIVITA' MODERATA"
                                statusColor = Color.rgb(255, 165, 0) // Arancio
                            }
                            tecMean >= 80 -> {
                                statusText = "ATTIVITA' ELEVATA!!\n" +
                                        "Possibili problemi nel calcolare la\n" +
                                        "posizione precisa"
                                statusColor = Color.RED
                            }
                            else -> {
                                statusText = "Status: UNKNOWN"
                                statusColor = Color.GRAY
                            }
                        }

                        // Imposta il testo e il colore per il messaggio di stato
                        tecStatusTextView.text = statusText
                        tecStatusTextView.setTextColor(statusColor)

                    } else {
                        tecValueTextView.text = "Errore nel recupero dei dati TEC"
                        tecStatusTextView.text = "Status: UNKNOWN"
                        tecStatusTextView.setTextColor(Color.GRAY)
                    }
                }
            }, 200) // Ritardo di 1000ms
        }

        gpsStatusHelper = GpsStatusHelper(this) { totalSatellites, usedSatellites, accuracy ->
            runOnUiThread {
                locationReceived = true // segna che la posizione è arrivata
                // Mostra precisione
                lastAccuracy = accuracy
                gpsAccuracyText.text = "📍 Precisione stimata: ±${accuracy.toInt()} m"

                // Calcolo del semaforo
                val semaforo = when {
                    usedSatellites >= 12 && accuracy <= 5 -> "🟢 Condizioni eccellenti"
                    usedSatellites >= 8 && accuracy <= 10 -> "🟡 Condizioni accettabili"
                    else -> "🔴 Condizioni critiche per volo GNSS"
                }
                gnssSummaryText.text = semaforo

                // Avviso se probabilmente sei al chiuso
                if (usedSatellites < 5 && accuracy > 20) {
                    gpsalertTextView.text = "⚠️ Lo smartphone sembra essere al chiuso..."
                    gpsalertTextView.setTextColor(getColor(R.color.red))
                } else {
                    gpsalertTextView.text = "✅ Buona ricezione GNSS\nRicezione attiva e stabile."
                    gpsalertTextView.setTextColor(getColor(R.color.green))
                }


                // Storico fix
                val timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val entry = "🕒 $timestamp — $usedSatellites sat, ±${accuracy.toInt()} m"
                fixHistory.add(0, entry)
                if (fixHistory.size > 5) fixHistory.removeAt(fixHistory.lastIndex)
                fixHistoryText.text = fixHistory.joinToString("\n")
            }
        }



        //Ricarica gpsalertTextView
        //gpsalertTextView.requestLayout()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            logDebug("DronePilotApp", "WeatherGpsActivity:  Chiamato gpsStatusHelper.startListening()")
            gpsStatusHelper.startListening()

            locationTimeoutHandler.postDelayed({
                if (!locationReceived) {
                    gpsalertTextView.text = "⚠️ Non riesco a ricevere una posizione GPS.\nLo smartphone sembra essere al chiuso o la ricezione è molto scarsa."
                    gpsalertTextView.setTextColor(getColor(R.color.red))
                    gpsAccuracyText.text = "📍 Precisione stimata: --"
                    gnssSummaryText.text = "🔴 Nessun fix ricevuto"
                    fixHistoryText.text = "Nessun dato disponibile"
                }
            }, 10000) // 10 secondi

        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_CODE)
        }

        // Ricarica TEC Values
        tecValueTextView.requestLayout()
        tecStatusTextView.requestLayout()

        // STATO RETI WIFI
        Handler(Looper.getMainLooper()).postDelayed({
            scanWifiFrequencies()
        }, 2000)


        // Pulsante per chiudere la finestra
        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun scanWifiFrequencies() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        // ⚠️ Controllo permesso FINE_LOCATION
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            wifiScanResultTextView.text = "⚠️ Permesso posizione necessario per analizzare le reti WiFi."
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE
            )
            return
        }

        try {
            val scanResults = wifiManager.scanResults

            val results2_4GHz = scanResults.filter { it.frequency in 2400..2500 }
            val results5GHz = scanResults.filter { it.frequency in 5000..5900 }

            val congested2_4GHz = results2_4GHz.groupBy { it.frequency }.filterValues { it.size >= 3 }
            val congested5GHz = results5GHz.groupBy { it.frequency }.filterValues { it.size >= 3 }

            val sb = StringBuilder()

            sb.append("📡 Reti 2.4GHz: ${results2_4GHz.size} — ")
            if (congested2_4GHz.isNotEmpty()) {
                sb.append("⚠️ Banda affollata. Assicurati di avere la banda 5GHz attiva.\n")
            } else {
                sb.append("✅ Banda 2.4GHz libera.\n")
            }

            sb.append("📡 Reti 5GHz: ${results5GHz.size} — ")
            if (congested5GHz.isNotEmpty()) {
                sb.append("⚠️ Anche la 5GHz ha molti segnali.\n")
            } else {
                sb.append("✅ Banda 5GHz utilizzabile.\n")
            }

            if (congested2_4GHz.isNotEmpty() && congested5GHz.isNotEmpty()) {
                sb.append("\n🚨 ATTENZIONE: entrambe le bande risultano congestionate.\n" +
                        "Se possibile, valuta di cambiare posizione di decollo.")
            }

            // Dettagli facoltativi
           // sb.append("\nSegnali visibili (dBm):\n")
           // (results2_4GHz + results5GHz)
           //     .sortedBy { it.level }
           //     .forEach {
           //         val band = if (it.frequency < 2500) "2.4GHz" else "5GHz"
           //         sb.append("• ${it.SSID.ifBlank { "(SSID nascosto)" }} @ $band → ${it.level} dBm\n")
           //     }

            wifiScanResultTextView.text = sb.toString()
            updateWifiChart(results2_4GHz, results5GHz)


        } catch (e: SecurityException) {
            wifiScanResultTextView.text = "❌ Errore: permesso negato alla scansione WiFi."
            e.printStackTrace()
        }



    }

    private fun updateWifiChart(results2_4GHz: List<ScanResult>, results5GHz: List<ScanResult>) {
        val entries24 = results2_4GHz.mapIndexed { index, result ->
            Entry(index.toFloat(), result.level.toFloat())
        }

        val entries5 = results5GHz.mapIndexed { index, result ->
            Entry(index.toFloat(), result.level.toFloat())
        }

        val dataSet24 = LineDataSet(entries24, "2.4GHz").apply {
            color = ColorTemplate.COLORFUL_COLORS[0]
            lineWidth = 2f
            setDrawCircles(false)
        }

        val dataSet5 = LineDataSet(entries5, "5GHz").apply {
            color = ColorTemplate.COLORFUL_COLORS[3]
            lineWidth = 2f
            setDrawCircles(false)
        }

        val lineData = LineData(dataSet24, dataSet5)
        wifiChart.data = lineData

        wifiChart.setBackgroundColor(Color.TRANSPARENT) // sfondo trasparente (o scuro, già fatto)
        wifiChart.setNoDataTextColor(Color.WHITE)

        wifiChart.axisRight.isEnabled = false

        wifiChart.axisLeft.apply {
            axisMinimum = -100f
            axisMaximum = -5f
            textColor = Color.WHITE       // ✅ Testo asse Y sinistro
            gridColor = Color.DKGRAY
        }

        wifiChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.WHITE       // ✅ Testo asse X
            gridColor = Color.DKGRAY
            setDrawAxisLine(true)
            setDrawGridLines(true)
        }

        wifiChart.legend.apply {
            textColor = Color.WHITE       // ✅ Testo legenda
            orientation = Legend.LegendOrientation.HORIZONTAL
        }

        wifiChart.description.isEnabled = false
        wifiChart.invalidate()

    }

    private fun setupMagneticChart() {
        magneticChart.setBackgroundColor(Color.TRANSPARENT)
        magneticChart.setNoDataTextColor(Color.WHITE)
        magneticChart.axisRight.isEnabled = false

        magneticChart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = 150f
            textColor = Color.WHITE
            gridColor = Color.DKGRAY
        }

        magneticChart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = Color.WHITE
            gridColor = Color.DKGRAY
            setDrawAxisLine(true)
            setDrawGridLines(true)
            labelRotationAngle = 0f
            setDrawLabels(false)  // facoltativo per restare pulito
        }

        magneticChart.legend.apply {
            textColor = Color.WHITE
            orientation = Legend.LegendOrientation.HORIZONTAL
        }

        magneticChart.description.isEnabled = false
    }

    override fun onDestroy() {
        super.onDestroy()
        gpsStatusHelper.stopListening()
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(magneticListener)
    }

    override fun onResume() {
        super.onResume()
        gpsStatusHelper.startListening()
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(magneticListener, magneticSensor, SensorManager.SENSOR_DELAY_UI)

    }

    override fun onPause() {
        super.onPause()
        gpsStatusHelper.stopListening()
        val sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(magneticListener)
    }

}





