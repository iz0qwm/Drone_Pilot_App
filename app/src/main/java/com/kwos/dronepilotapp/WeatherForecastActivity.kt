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


class WeatherForecastActivity : AppCompatActivity() {

    private lateinit var gpsStatusHelper: GpsStatusHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather_forecast)

        val weatherDetails: TextView = findViewById(R.id.weather_details)
        val meteogramOneImage: ImageView = findViewById(R.id.weather_meteogramOne_general)
        val meteogramImage: ImageView = findViewById(R.id.weather_meteogram_general)
        val closeButton: Button = findViewById(R.id.close_weather_button)
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

        MeteoManager.getMeteoData(lat, lon) { meteoData ->
            Handler(Looper.getMainLooper()).postDelayed({
                runOnUiThread {
                    if (meteoData != null) {
                        weatherDetails.text =
                            "Temp. Min: ${meteoData.temperature_min}°C - Max: ${meteoData.temperature_max}°C\n" +
                                    "Vento Min: ${meteoData.wind_speedmin} km/h\n" +
                                    "Vento Medio: ${meteoData.wind_speedmean} km/h\n" +
                                    "Vento Max: ${meteoData.wind_speedmax} km/h\n" +
                                    "Umidità: ${meteoData.humidity}\n%" +
                                    "Possibilità di precipitazioni: ${meteoData.precipitation_probability}% "

                        // Aggiungere la logica di allerta basata sul vento
                        if (meteoData.wind_speedmin > 30 || meteoData.wind_speedmean > 30 || meteoData.wind_speedmax > 30) {
                            alertTextView.text = "ALERT VENTO FORTE!!!"
                            alertTextView.setTextColor(getColor(R.color.red)) // Imposta il testo in rosso
                        }
                        // Aggiungere la logica di allerta pioggia
                        else if (meteoData.precipitation_probability > 70) {
                            alertTextView.text = "ALERT POSSIBILE PIOGGIA!!!"
                            alertTextView.setTextColor(getColor(R.color.blue)) // Imposta il testo in blu
                        }
                        // Altrimenti se il vento è sotto i limiti, rimuovi l'allerta
                        else {
                            alertTextView.text = "PUOI FAR VOLARE L'UAS"
                            alertTextView.setTextColor(getColor(R.color.green)) // Imposta il testo in verde
                        }
                    } else {
                        weatherDetails.text = "Dati meteo non disponibili"
                    }
                }
            }, 1000) // Ritardo di 500ms
        }

        // Carico l'immagine del meteogramma all in One
        val meteogramOneUrl = MeteoManager.getMeteogramOneImageUrl(lat, lon)
        Picasso.get().load(meteogramOneUrl).into(meteogramOneImage)
        // Carico l'immagine del meteogramma
        val meteogramUrl = MeteoManager.getMeteogramImageUrl(lat, lon)
        Picasso.get().load(meteogramUrl).into(meteogramImage)

        gpsStatusHelper = GpsStatusHelper(this) { totalSatellites, usedSatellites ->
            runOnUiThread {
                val statusText = "Satelliti visibili: $totalSatellites, Usati per il fix: $usedSatellites"
                findViewById<TextView>(R.id.gpsStatusTextView).text = statusText
                // Aggiungi il controllo per la bassa ricezione GPS
                if (totalSatellites < 15) {
                    alertTextView.text = "ALERT BASSA RICEZIONE GPS!!!"
                    alertTextView.setTextColor(getColor(R.color.red)) // Imposta il testo in rosso
                }
            }
        }

        gpsStatusHelper.startListening()


        meteogramOneImage.setOnClickListener {
            val intent = Intent(this, FullScreenImageActivity::class.java)
            intent.putExtra("IMAGE_URL", meteogramOneUrl)
            startActivity(intent)
        }
        //

        meteogramImage.setOnClickListener {
            val intent = Intent(this, FullScreenImageActivity::class.java)
            intent.putExtra("IMAGE_URL", meteogramUrl)
            startActivity(intent)
        }
        //

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



