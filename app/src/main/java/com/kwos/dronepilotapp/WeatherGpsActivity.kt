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

class WeatherGpsActivity : AppCompatActivity() {

    private lateinit var gpsStatusHelper: GpsStatusHelper
    private lateinit var tecValueTextView: TextView
    private lateinit var tecStatusTextView: TextView
    private lateinit var tecTitleTextView: TextView

    private val REQUEST_CODE = 1001


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

        gpsStatusHelper = GpsStatusHelper(this) { totalSatellites, usedSatellites ->
            //logDebug("DronePilotApp", "gpsStatusHelper: Callback ricevuta: totalSatellites=$totalSatellites, usedSatellites=$usedSatellites")
            Handler(Looper.getMainLooper()).postDelayed({
                runOnUiThread {
                    val statusText = "Satelliti visibili: $totalSatellites, Usati per il fix: $usedSatellites"
                    findViewById<TextView>(R.id.gpsStatusTextView).text = statusText
                    // Aggiungi il controllo per la bassa ricezione GPS
                    if (usedSatellites < 10) {
                        gpsalertTextView.text = "BASSA RICEZIONE GPS!!!\n" +
                                "Il tuo smartphone usa pochi satelliti per il fix.\n"+
                                "Sei in interno o vi sono alcuni problemi in questa zona."
                        gpsalertTextView.setTextColor(getColor(R.color.red)) // Imposta il testo in rosso
                    } else {
                        gpsalertTextView.text = "BUONA RICEZIONE GPS\n" +
                                "Il tuo smartphone vede i satelliti GPS.\n"+
                                "Sei all'esterno e le condizioni di ricezione sono buone."
                        gpsalertTextView.setTextColor(getColor(R.color.green)) // Imposta il testo in verde
                    }
                }
            }, 100) // Ritardo di 1000ms
        }

        //Ricarica gpsalertTextView
        //gpsalertTextView.requestLayout()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            logDebug("DronePilotApp", "WeatherGpsActivity:  Chiamato gpsStatusHelper.startListening()")
            gpsStatusHelper.startListening()
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





