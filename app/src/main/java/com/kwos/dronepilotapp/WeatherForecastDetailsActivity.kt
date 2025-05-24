package com.kwos.dronepilotapp

//per il padding

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.ln
import kotlin.math.roundToInt

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


        val meteogramOneImage: ImageView = findViewById(R.id.weather_meteogramOne_general)
        val meteogramImage: ImageView = findViewById(R.id.weather_meteogram_general)
        val closeButton: Button = findViewById(R.id.close_weather_button)
        weather_meteogram = findViewById(R.id.weather_meteogram)

        // Recupero le coordinate passate dall'Activity principale
        val lat = intent.getDoubleExtra("LATITUDE", 0.0)
        val lon = intent.getDoubleExtra("LONGITUDE", 0.0)



        hourlyWeatherRecyclerView = findViewById(R.id.hourlyWeatherRecyclerView)
        hourlyWeatherRecyclerView.layoutManager = LinearLayoutManager(this)

        OpenWeatherManager.getHourlyWeather(lat, lon) { forecastList ->
            runOnUiThread {
                if (forecastList != null) {
                    hourlyWeatherAdapter = HourlyWeatherAdapter(forecastList)
                    hourlyWeatherRecyclerView.adapter = hourlyWeatherAdapter

                    val windProfileTable = findViewById<TableLayout>(R.id.windProfileTable)
                    val roughness = 0.03
                    val heights = listOf(10, 50, 100, 200)

                    forecastList.take(6).forEach { forecast ->
                        val hour = forecast.dt.substringAfter(" ")
                        val wind10 = forecast.windSpeed / 3.6
                        val gust10 = forecast.windGust / 3.6
                        val dir = forecast.windDirection

                        val row = TableRow(this@WeatherForecastDetailsActivity)
                        val hourText = TextView(this@WeatherForecastDetailsActivity).apply {
                            text = hour
                            setPadding(6, 6, 6, 6)
                        }
                        row.addView(hourText)

                        for (z in heights) {
                            val estimatedSpeed = wind10 * (ln(z / roughness) / ln(10.0 / roughness))
                            val estimatedGust = gust10 * (ln(z / roughness) / ln(10.0 / roughness))
                            val speedKmh = (estimatedSpeed * 3.6).roundToInt()
                            val gustKmh = (estimatedGust * 3.6).roundToInt()

                            val directionArrow = getArrowFromDegrees(dir)

                            val cell = TextView(this@WeatherForecastDetailsActivity).apply {
                                text = "$speedKmh km/h $directionArrow\n💨 $gustKmh km/h"
                                setPadding(6, 6, 6, 6)
                                textAlignment = View.TEXT_ALIGNMENT_CENTER
                                setTextColor(getColor(android.R.color.black))
                                setBackgroundColor(getWindColor(speedKmh))
                                setLines(2) // massimo 2 righe
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)

                                isSingleLine = false
                                ellipsize = null
                            }

                            row.addView(cell)
                        }

                        windProfileTable.addView(row)
                    }

                    val riskAlertView = findViewById<TextView>(R.id.wind_risk_alert)
                    val optimalTimesView = findViewById<TextView>(R.id.optimal_flight_times)

                    val criticalForecast = forecastList.take(12).any { it.windSpeed > 30 || it.windGust > 40 }

                    if (criticalForecast) {
                        riskAlertView.text = "⚠️ Attenzione: il vento o le raffiche superano i limiti consigliati per il volo."
                        riskAlertView.visibility = View.VISIBLE
                    } else {
                        riskAlertView.visibility = View.GONE
                    }

                    // Cerca fasce orarie con vento < 10 km/h e raffiche < 20 km/h
                    val optimalHours = forecastList
                        .take(12)
                        .filter { it.windSpeed <= 10 && it.windGust <= 20 }
                        .map { it.dt.substringAfter(" ") }

                    if (optimalHours.isNotEmpty()) {
                        val grouped = groupConsecutiveTimes(optimalHours)
                        optimalTimesView.text = "⏱️ Orari consigliati per volare: ${grouped.joinToString(", ")}"
                    } else {
                        optimalTimesView.text = "⏱️ Nessuna fascia oraria ottimale rilevata nelle prossime ore."
                    }
                }

            }
        }



        // Meteogrammi di OpenWeather
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

    private fun getArrowFromDegrees(deg: Int): String {
        return when (deg) {
            in 0..22 -> "⬇️"
            in 23..67 -> "↙️"
            in 68..112 -> "⬅️"
            in 113..157 -> "↖️"
            in 158..202 -> "⬆️"
            in 203..247 -> "↗️"
            in 248..292 -> "➡️"
            in 293..337 -> "↘️"
            else -> "⬇️"
        }
    }

    private fun getWindColor(speedKmh: Int): Int {
        return when {
            speedKmh > 40 -> Color.parseColor("#FF6347") // Rosso
            speedKmh > 30 -> Color.parseColor("#FFA500") // Arancione
            speedKmh > 20 -> Color.parseColor("#FFD700") // Giallo
            speedKmh > 10 -> Color.parseColor("#D0F0C0") // Verde chiaro
            else -> Color.parseColor("#DCDCDC") // Default
        }
    }

    private fun groupConsecutiveTimes(times: List<String>): List<String> {
        if (times.isEmpty()) return emptyList()

        val result = mutableListOf<String>()
        var start = times.first()
        var prev = times.first()

        fun areConsecutive(a: String, b: String): Boolean {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val aDate = format.parse(a)
            val bDate = format.parse(b)
            val diff = (bDate.time - aDate.time) / (60 * 1000)
            return diff == 60L
        }

        for (i in 1 until times.size) {
            val current = times[i]
            if (!areConsecutive(prev, current)) {
                result.add(if (start != prev) "$start–$prev" else start)
                start = current
            }
            prev = current
        }

        result.add(if (start != prev) "$start–$prev" else start)
        return result
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}





