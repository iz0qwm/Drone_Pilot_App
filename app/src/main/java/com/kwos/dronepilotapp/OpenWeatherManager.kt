package com.kwos.dronepilotapp

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object OpenWeatherManager {
    private const val API_KEY = "a731aea1a28eb47fc65b320835c07b5f" // Sostituisci con la tua API key di OpenWeather
    private const val BASE_URL = "https://api.openweathermap.org/data/3.0/onecall"

    fun getHourlyWeather(lat: Double, lon: Double, callback: (List<HourlyForecast>?) -> Unit) {
        val url = "$BASE_URL?exclude=current,minutely,daily,alerts&lat=$lat&lon=$lon&units=metric&appid=$API_KEY"
        logDebug("DronePilotApp", "OpenWeatherManager:  getHourlyWeather: Vado su:  $url")
        val request = Request.Builder()
            .url(url)
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DronePilotApp", "Errore nella richiesta meteo", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("DronePilotApp", "Risposta non riuscita: ${response.code}")
                        callback(null)
                        return
                    }

                    val responseData = response.body?.string()
                    if (responseData != null) {
                        try {
                            val json = JSONObject(responseData)
                            val hourlyArray = json.getJSONArray("hourly")
                            val forecastList = mutableListOf<HourlyForecast>()

                            for (i in 0 until hourlyArray.length()) {
                                val item = hourlyArray.getJSONObject(i)
                                val dt = item.getLong("dt")
                                val temp = item.getDouble("temp")
                                val pressure = item.getDouble("pressure")
                                val windSpeed = item.getDouble("wind_speed")
                                val windGust = item.optDouble("wind_gust", 0.0)
                                val windDeg = item.getInt("wind_deg")
                                val clouds = item.getInt("clouds")

                                val formattedDate = formatDateTime(dt)

                                val tempRounded = (temp * 10).roundToInt() / 10.0  // Arrotonda a una cifra decimale
                                val pressureRounded = pressure.roundToInt()  // Arrotondamento
                                val windSpeedInKmhRounded = (windSpeed * 3.6).roundToInt()  // Arrotondamento
                                val windGustInKmhRounded = (windGust * 3.6).roundToInt()    // Arrotondamento


                                val forecast = HourlyForecast(
                                    dt = formattedDate,
                                    temp = tempRounded,
                                    pressure = pressureRounded,
                                    windSpeed = windSpeedInKmhRounded,
                                    windGust = windGustInKmhRounded,
                                    clouds = clouds,
                                    windDirection = windDeg
                                )

                                forecastList.add(forecast)
                            }

                            callback(forecastList)
                        } catch (e: Exception) {
                            Log.e("DronePilotApp", "Errore nel parsing dei dati meteo", e)
                            callback(null)
                        }
                    } else {
                        callback(null)
                    }
                }
            }
        })
    }

    private fun formatDateTime(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    fun getDailyWeather(lat: Double, lon: Double, giorniDopo: Int, callback: (DailyWeather?) -> Unit) {
        val url = "https://api.openweathermap.org/data/3.0/onecall?lat=$lat&lon=$lon&exclude=current,minutely,hourly,alerts&units=metric&lang=it&appid=$API_KEY"
        logDebug("DronePilotApp", "OpenWeatherManager: getDailyWeather: $url")

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DronePilotApp", "Errore meteo giornaliero", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("DronePilotApp", "getDailyWeather: Risposta non valida: ${response.code}")
                        callback(null)
                        return
                    }

                    val json = JSONObject(response.body?.string() ?: "")
                    val dailyArray = json.getJSONArray("daily")

                    if (giorniDopo >= dailyArray.length()) {
                        callback(null)
                        return
                    }

                    val item = dailyArray.getJSONObject(giorniDopo)

                    val temp = item.getJSONObject("temp")
                    val tempMin = temp.getDouble("min").roundToInt()
                    val tempMax = temp.getDouble("max").roundToInt()

                    val weather = item.getJSONArray("weather").getJSONObject(0)
                    val condition = weather.getString("description").replaceFirstChar { it.uppercase() }

                    val windSpeed = (item.optDouble("wind_speed", 0.0) * 3.6).roundToInt()
                    val windGust = (item.optDouble("wind_gust", 0.0) * 3.6).roundToInt()

                    val result = DailyWeather(
                        tempMin = tempMin,
                        tempMax = tempMax,
                        condition = condition,
                        windSpeed = windSpeed,
                        windGust = windGust
                    )

                    callback(result)
                }
            }
        })
    }

}


// Data class per il meteo orario
data class HourlyForecast(
    val dt: String,
    val temp: Double,
    val pressure: Int,
    val windSpeed: Int,
    val windGust: Int,
    val clouds: Int,
    val windDirection: Int

)

data class DailyWeather(
    val tempMin: Int,
    val tempMax: Int,
    val condition: String,
    val windSpeed: Int,
    val windGust: Int
)
