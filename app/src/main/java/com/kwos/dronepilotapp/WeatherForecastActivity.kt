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
import java.util.Locale

class WeatherForecastActivity : AppCompatActivity() {


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

        val weatherDetails: TextView = findViewById(R.id.weather_details)
        val closeButton: Button = findViewById(R.id.close_weather_button)
        val gpsButton: Button = findViewById(R.id.btn_gps)
        val forecastdetailsButton: Button = findViewById(R.id.btn_forecast_details)
        val locationText: TextView = findViewById(R.id.weather_location)
        val alertTextView: TextView = findViewById(R.id.alertTextView)

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
                        meteoData.wind_speedmin > 15 || meteoData.wind_speedmean > 15 || meteoData.wind_speedmax > 15 -> {
                            alertTextView.text = getString(R.string.alert_vento_moderato)
                            alertTextView.setTextColor(Color.rgb(255, 165, 0)) // Arancione
                        }
                        meteoData.wind_speedmin > 20 || meteoData.wind_speedmean > 20 || meteoData.wind_speedmax > 20 -> {
                            alertTextView.text = getString(R.string.alert_vento_forte)
                            alertTextView.setTextColor(ContextCompat.getColor(this, R.color.red))
                        }
                        meteoData.precipitation_probability > 70 && meteoData.humidity > 70 -> {
                            alertTextView.text = getString(R.string.alert_pioggia_forte)
                            alertTextView.setTextColor(ContextCompat.getColor(this, R.color.red)) // Rosso
                        }
                        meteoData.precipitation_probability > 40 && meteoData.wind_speedmean < 10 && meteoData.humidity > 70 -> {
                            alertTextView.text = getString(R.string.alert_pioggia_moderata)
                            alertTextView.setTextColor(Color.rgb(255, 165, 0)) // Arancione
                        }
                        meteoData.precipitation_probability > 25 && meteoData.wind_speedmean < 10 && meteoData.humidity > 70 -> {
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

            }, 1000) // Ritardo di 2000ms
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



