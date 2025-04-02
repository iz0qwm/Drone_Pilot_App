package com.kwos.dronepilotapp

//per il padding

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.picasso.Picasso

class WeatherForecastDetailsActivity : AppCompatActivity() {


    private lateinit var weather_meteogram: TextView

    private lateinit var hourlyWeatherRecyclerView: RecyclerView
    private lateinit var hourlyWeatherAdapter: HourlyWeatherAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Abilita l'invio dei crash su Firebase console
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        setContentView(R.layout.activity_weather_forecastdetails)

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


        val meteogramOneImage: ImageView = findViewById(R.id.weather_meteogramOne_general)
        val meteogramImage: ImageView = findViewById(R.id.weather_meteogram_general)
        val closeButton: Button = findViewById(R.id.close_weather_button)
        weather_meteogram = findViewById(R.id.weather_meteogram)

        // Recupero le coordinate passate dall'Activity principale
        val lat = intent.getDoubleExtra("LATITUDE", 0.0)
        val lon = intent.getDoubleExtra("LONGITUDE", 0.0)

        //setContentView(R.layout.activity_dashboard)
        supportActionBar?.hide()


        hourlyWeatherRecyclerView = findViewById(R.id.hourlyWeatherRecyclerView)
        hourlyWeatherRecyclerView.layoutManager = LinearLayoutManager(this)

        OpenWeatherManager.getHourlyWeather(lat, lon) { forecastList ->
            runOnUiThread {
                if (forecastList != null) {
                    hourlyWeatherAdapter = HourlyWeatherAdapter(forecastList)
                    hourlyWeatherRecyclerView.adapter = hourlyWeatherAdapter
                }
            }
        }

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(object : Runnable {
            override fun run() {
                    // Ora che i dati sono stati caricati, carico le immagini
                    val meteogramOneUrl = MeteoManager.getMeteogramOneImageUrl(lat, lon)
                    Picasso.get().load(meteogramOneUrl).into(meteogramOneImage)

                    val meteogramUrl = MeteoManager.getMeteogramImageUrl(lat, lon)
                    Picasso.get().load(meteogramUrl).into(meteogramImage)

                    // Imposto il click per visualizzare le immagini a schermo intero
                    meteogramOneImage.setOnClickListener {
                        val intent = Intent(this@WeatherForecastDetailsActivity, FullScreenImageActivity::class.java)
                        intent.putExtra("IMAGE_URL", meteogramOneUrl)
                        startActivity(intent)
                    }

                    meteogramImage.setOnClickListener {
                        val intent = Intent(this@WeatherForecastDetailsActivity, FullScreenImageActivity::class.java)
                        intent.putExtra("IMAGE_URL", meteogramUrl)
                        startActivity(intent)
                    }
            }
        }, 500)


        // Pulsante per chiudere la finestra
        closeButton.setOnClickListener {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}





