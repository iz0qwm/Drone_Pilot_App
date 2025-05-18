package com.kwos.dronepilotapp

//per il padding

import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.Manifest
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Abilita l'invio dei crash su Firebase console
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        setContentView(R.layout.activity_weather_gps)

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


        val closeButton: Button = findViewById(R.id.close_gps_button)

        val gpsalertTextView: TextView = findViewById(R.id.gpsalertTextView)
        tecValueTextView = findViewById(R.id.tecValueTextView)
        tecStatusTextView = findViewById(R.id.tecStatusTextView)
        tecTitleTextView = findViewById(R.id.tecTitleTextView)
        gpsAccuracyText = findViewById(R.id.gps_accuracy_text)
        gnssSummaryText = findViewById(R.id.gnss_summary)
        fixHistoryText = findViewById(R.id.fix_history)

        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()




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
                            tecMean < 125 -> {
                                statusText = "CONDIZIONI DI CALMA"
                                statusColor = Color.rgb(19, 117, 13) // VERDE SCURO
                            }
                            tecMean >= 125 && tecMean < 175 -> {
                                statusText = "ATTIVITA' MODERATA"
                                statusColor = Color.rgb(255, 165, 0) // Arancio
                            }
                            tecMean >= 175 -> {
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



        // Pulsante per chiudere la finestra
        closeButton.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gpsStatusHelper.stopListening()
    }

    override fun onResume() {
        super.onResume()
        gpsStatusHelper.startListening()
    }

    override fun onPause() {
        super.onPause()
        gpsStatusHelper.stopListening()
    }

}





