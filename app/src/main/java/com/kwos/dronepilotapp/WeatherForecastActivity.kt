package com.kwos.dronepilotapp

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.picasso.Picasso
import android.content.Intent
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import java.util.Locale
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.view.View
import android.widget.ProgressBar
import androidx.core.content.ContextCompat


class WeatherForecastActivity : AppCompatActivity() {

    private lateinit var gpsStatusHelper: GpsStatusHelper
    private lateinit var tecValueTextView: TextView
    private lateinit var tecStatusTextView: TextView
    private lateinit var tecTitleTextView: TextView
    private lateinit var weather_meteogram: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_forecast)

        // Recupero la ProgressBar
        val weatherProgressBar: ProgressBar = findViewById(R.id.weatherProgressBar)

        // Mostro la ProgressBar prima di caricare i dati
        weatherProgressBar.visibility = View.VISIBLE

        val weatherDetails: TextView = findViewById(R.id.weather_details)
        val meteogramOneImage: ImageView = findViewById(R.id.weather_meteogramOne_general)
        val meteogramImage: ImageView = findViewById(R.id.weather_meteogram_general)
        val closeButton: Button = findViewById(R.id.close_weather_button)
        val locationText: TextView = findViewById(R.id.weather_location)
        val alertTextView: TextView = findViewById(R.id.alertTextView)
        val gpsalertTextView: TextView = findViewById(R.id.gpsalertTextView)
        tecValueTextView = findViewById(R.id.tecValueTextView)
        tecStatusTextView = findViewById(R.id.tecStatusTextView)
        tecTitleTextView = findViewById(R.id.tecTitleTextView)
        weather_meteogram = findViewById(R.id.weather_meteogram)

        // Recupero le coordinate passate dall'Activity principale
        val lat = intent.getDoubleExtra("LATITUDE", 0.0)
        val lon = intent.getDoubleExtra("LONGITUDE", 0.0)

        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        //Prendo il nome del luogo
        getLocationName(this, lat, lon) { locationName ->
            locationText.text = locationName
        }

        // Recupera i dati meteo
        MeteoManager.getMeteoData(lat, lon) { meteoData ->
            Handler(Looper.getMainLooper()).postDelayed({
                if (meteoData != null) {
                    // Usa getString per evitare concatenazione manuale delle stringhe
                    weatherDetails.text = getString(
                        R.string.weather_info,
                        meteoData.temperature_min,
                        meteoData.temperature_max,
                        meteoData.wind_speedmin,
                        meteoData.wind_speedmean,
                        meteoData.wind_speedmax,
                        meteoData.humidity,
                        meteoData.precipitation_probability
                    )

                    // Logica di allerta basata sul vento e precipitazioni
                    when {
                        meteoData.wind_speedmin > 30 || meteoData.wind_speedmean > 30 || meteoData.wind_speedmax > 30 -> {
                            alertTextView.text = getString(R.string.alert_vento_forte)
                            alertTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
                        }
                        meteoData.precipitation_probability > 70 && meteoData.humidity > 70 -> {
                            alertTextView.text = getString(R.string.alert_pioggia_forte)
                            alertTextView.setTextColor(Color.rgb(255, 165, 0)) // Arancione
                        }
                        meteoData.precipitation_probability > 40 && meteoData.wind_speedmean < 10 && meteoData.humidity > 70 -> {
                            alertTextView.text = getString(R.string.alert_pioggia_moderata)
                            alertTextView.setTextColor(Color.rgb(255, 165, 0)) // Arancione
                        }
                        meteoData.precipitation_probability > 20 && meteoData.wind_speedmean < 10 && meteoData.humidity > 70 -> {
                            alertTextView.text = getString(R.string.alert_pioggia_leggera)
                            alertTextView.setTextColor(Color.rgb(255, 165, 0)) // Arancione
                        }
                        else -> {
                            alertTextView.text = getString(R.string.alert_condizioni_ok)
                            alertTextView.setTextColor(ContextCompat.getColor(this, R.color.green))
                        }
                    }

                } else {
                    weatherDetails.text = getString(R.string.weather_data_not_available)
                }

                // Nascondo la ProgressBar perché i dati sono caricati
                weatherProgressBar.visibility = View.GONE

            }, 2000) // Ritardo di 2000ms
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

                        // Nascondo la ProgressBar
                        weatherProgressBar.visibility = View.GONE
                    } else {
                        tecValueTextView.text = "Errore nel recupero dei dati TEC"
                        tecStatusTextView.text = "Status: UNKNOWN"
                        tecStatusTextView.setTextColor(Color.GRAY)
                        // Nascondo la ProgressBar
                        weatherProgressBar.visibility = View.GONE
                    }
                }
            }, 2000) // Ritardo di 1000ms
        }


        gpsStatusHelper = GpsStatusHelper(this) { totalSatellites, usedSatellites ->
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
            }, 2000) // Ritardo di 1000ms
        }


        gpsStatusHelper.startListening()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (weatherProgressBar.visibility == View.GONE) {
                    // Ora che i dati sono stati caricati, carico le immagini
                    val meteogramOneUrl = MeteoManager.getMeteogramOneImageUrl(lat, lon)
                    Picasso.get().load(meteogramOneUrl).into(meteogramOneImage)

                    val meteogramUrl = MeteoManager.getMeteogramImageUrl(lat, lon)
                    Picasso.get().load(meteogramUrl).into(meteogramImage)

                    // Imposto il click per visualizzare le immagini a schermo intero
                    meteogramOneImage.setOnClickListener {
                        val intent = Intent(this@WeatherForecastActivity, FullScreenImageActivity::class.java)
                        intent.putExtra("IMAGE_URL", meteogramOneUrl)
                        startActivity(intent)
                    }

                    meteogramImage.setOnClickListener {
                        val intent = Intent(this@WeatherForecastActivity, FullScreenImageActivity::class.java)
                        intent.putExtra("IMAGE_URL", meteogramUrl)
                        startActivity(intent)
                    }

                } else {
                    // Riprova tra 500ms se la ProgressBar è ancora visibile
                    handler.postDelayed(this, 500)
                }
            }
        }, 2500)


        // Pulsante per chiudere la finestra
        closeButton.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gpsStatusHelper.stopListening()
    }
}

private fun getLocationName(activity: WeatherForecastActivity, lat: Double, lon: Double, callback: (String) -> Unit) {
    val geocoder = Geocoder(activity, Locale.getDefault())

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // Metodo asincrono per Android 13+
        geocoder.getFromLocation(lat, lon, 1, object : GeocodeListener {
            override fun onGeocode(addresses: MutableList<android.location.Address>) {
                val locationName = if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val city = address.locality ?: ""
                    val country = address.countryName ?: ""
                    "$city, $country".trim().removePrefix(",")
                } else {
                    "Località sconosciuta"
                }
                callback(locationName)
            }

            override fun onError(errorMessage: String?) {
                callback("Località non disponibile")
            }
        })
    } else {
        // Metodo sincrono per versioni precedenti
        Thread {
            try {
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                val locationName = if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val city = address.locality ?: ""
                    val country = address.countryName ?: ""
                    "$city, $country".trim().removePrefix(",")
                } else {
                    "Località sconosciuta"
                }

                activity.runOnUiThread {
                    callback(locationName)
                }
            } catch (e: Exception) {
                activity.runOnUiThread {
                    callback("Località non disponibile")
                }
            }
        }.start()
    }

}



