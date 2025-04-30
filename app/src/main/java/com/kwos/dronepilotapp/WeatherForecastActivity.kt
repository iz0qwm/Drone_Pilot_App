package com.kwos.dronepilotapp

//per il padding

import android.content.Intent
import android.graphics.Color
import android.location.Geocoder
import android.location.Geocoder.GeocodeListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.firebase.crashlytics.FirebaseCrashlytics
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale

class WeatherForecastActivity : AppCompatActivity() {
    private val TAG = "DronePilotApp"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Abilita l'invio dei crash su Firebase console
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        setContentView(R.layout.activity_weather_forecast)

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

        // Recupero la ProgressBar
        val weatherProgressBar: ProgressBar = findViewById(R.id.weatherProgressBar)

        // Mostro la ProgressBar prima di caricare i dati
        weatherProgressBar.visibility = View.VISIBLE

        //val weatherDetails: TextView = findViewById(R.id.weather_details)
        val closeButton: Button = findViewById(R.id.close_weather_button)
        val gpsButton: Button = findViewById(R.id.btn_gps)
        val forecastdetailsButton: Button = findViewById(R.id.btn_forecast_details)
        val locationText: TextView = findViewById(R.id.weather_location)
        val alertTextView: TextView = findViewById(R.id.alertTextView)

        val temperatureText = findViewById<TextView>(R.id.temperatureText)
        val temperatureMinText = findViewById<TextView>(R.id.temperatureMinText)
        val temperatureMaxText = findViewById<TextView>(R.id.temperatureMaxText)
        val windMinText = findViewById<TextView>(R.id.windMinText)
        val windMeanText = findViewById<TextView>(R.id.windMeanText)
        val windMaxText = findViewById<TextView>(R.id.windMaxText)
        val humidityText = findViewById<TextView>(R.id.humidityText)
        val precipitationText = findViewById<TextView>(R.id.precipitationText)


        // Recupero le coordinate passate dall'Activity principale
        val lat = intent.getDoubleExtra("LATITUDE", 0.0)
        val lon = intent.getDoubleExtra("LONGITUDE", 0.0)


        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()

        //Prendo il nome del luogo
        getLocationName(this, lat, lon) { locationName ->
            Handler(Looper.getMainLooper()).postDelayed({
                locationText.text = locationName
            }, 500)
        }


        // Recupera i dati meteo
        MeteoManager.getMeteoData(lat, lon) { meteoData ->
            Handler(Looper.getMainLooper()).postDelayed({
                if (meteoData != null) {
                    // Imposta le temperature
                    temperatureText.text = "Ora: ${meteoData.temperature}"
                    temperatureMinText.text = "Min: ${meteoData.temperature_min}"
                    temperatureMaxText.text = "Max: ${meteoData.temperature_max}°C"

                    // Imposta vento
                    windMinText.text = "Min: ${meteoData.wind_speedmin}"
                    windMeanText.text = "Med: ${meteoData.wind_speedmean}"
                    windMaxText.text = "Max: ${meteoData.wind_speedmax} km/h"

                    // Imposta umidità
                    humidityText.text = "${meteoData.humidity}%"

                    // Imposta precipitazioni
                    precipitationText.text = "${meteoData.precipitation_probability}% - Previsti: ${meteoData.convective_precipitation} mm"

                    // Scegli l'icona meteo in base a precipitazioni e vento
                    val isRainLikely = meteoData.precipitation_probability > 50
                    val isWindStrong = meteoData.wind_speedmean > 15

                    val emoji = when {
                        isRainLikely -> "🌧️"
                        isWindStrong -> "💨"
                        else -> "☀️"
                    }

                    val descrizione = when {
                        isRainLikely -> "Possibile pioggia"
                        isWindStrong -> "Vento forte"
                        else -> "Cielo sereno"
                    }

                    val weatherDescription: TextView = findViewById(R.id.weatherDescription)
                    weatherDescription.text = "$emoji $descrizione"


                    // Logica di allerta
                    when {
                        meteoData.wind_speedmean > 20 || meteoData.wind_speedmax > 20 -> {
                            alertTextView.text = "ALERT !! VENTO FORTE"
                            alertTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
                        }
                        meteoData.wind_speedmean > 15 -> {
                            alertTextView.text = "ALLERTA VENTO MODERATO"
                            alertTextView.setTextColor(Color.rgb(255, 165, 0)) // Arancione
                        }
                        meteoData.precipitation_probability > 70 && meteoData.humidity > 70 -> {
                            alertTextView.text = "ALERT !! STA PIOVENDO O PIOVERÀ"
                            alertTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
                        }
                        meteoData.precipitation_probability > 50 -> {
                            alertTextView.text = "STA PIOVENDO O PIOVERÀ. ATTENZIONE"
                            alertTextView.setTextColor(Color.rgb(255, 165, 0))
                        }
                        meteoData.precipitation_probability > 25 -> {
                            alertTextView.text = "ARIA INSTABILE. STAI ATTENTO."
                            alertTextView.setTextColor(Color.rgb(255, 165, 0))
                        }
                        else -> {
                            alertTextView.text = "LE CONDIZIONI METEO SONO BUONE"
                            alertTextView.setTextColor(ContextCompat.getColor(this, R.color.green))
                        }
                    }
                } else {
                    // Nessun dato disponibile
                    alertTextView.text = "Dati meteo non disponibili"
                    alertTextView.setTextColor(ContextCompat.getColor(this, R.color.gray))
                }

                // Nascondi la progress bar
                weatherProgressBar.visibility = View.GONE

            }, 1000)
        }

        // Recupera dati alba e tramonto
        MeteoManager.getDaylightData(lat, lon) { results ->
            if (results != null) {
                Handler(Looper.getMainLooper()).post {
                    findViewById<TextView>(R.id.sunriseText).text = "☀️ Alba: ${results.getString("sunrise")}"
                    findViewById<TextView>(R.id.sunsetText).text = "🌇 Tramonto: ${results.getString("sunset")}"
                    findViewById<TextView>(R.id.civilTwilightBeginText).text = "🌅 Inizio Crepuscolo: ${results.getString("civil_twilight_begin")}"
                    findViewById<TextView>(R.id.civilTwilightEndText).text = "🌆 Fine Crepuscolo: ${results.getString("civil_twilight_end")}"
                }
            } else {
                logError(TAG, "Errore caricamento orari luce giorno")
            }
        }




        gpsButton.setOnClickListener {
            startActivity(Intent(this, WeatherGpsActivity::class.java))
        }

        forecastdetailsButton.setOnClickListener {
            //Rimetto lat e lon su intent per le activity successive
            val intent = Intent(this, WeatherForecastDetailsActivity::class.java)
            intent.putExtra("LATITUDE", lat)
            intent.putExtra("LONGITUDE", lon)
            startActivity(intent) // Usa l'intent corretto con i dati
        }

        // Pulsante per chiudere la finestra
        closeButton.setOnClickListener {
            finish()
        }


        //Ricarica alertTextView
        alertTextView.requestLayout()
        locationText.requestLayout()  // Forza un ridisegno della TextView


    }

    override fun onDestroy() {
        super.onDestroy()
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



