package com.kwos.dronepilotapp

import android.util.Log
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object OpenWeatherManager {
    private const val API_KEY = "1b4656e0d2c4084cb53766425375c83c" // Sostituisci con la tua API key di OpenWeather
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/forecast/hourly"

    fun getHourlyWeather(lat: Double, lon: Double, callback: (List<HourlyForecast>?) -> Unit) {
        val url = "$BASE_URL?lat=$lat&lon=$lon&units=metric&appid=$API_KEY"

        val request = Request.Builder()
            .url(url)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                logError("DronePilotApp", "OpenWeatherManager: Errore nella richiesta meteo", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        logError("DronePilotApp", "OpenWeatherManager: Risposta non riuscita: ${response.code}")
                        callback(null)
                        return
                    }

                    val responseData = response.body?.string()
                    if (responseData != null) {
                        try {
                            val json = JSONObject(responseData)
                            val list = json.getJSONArray("list")
                            val forecastList = mutableListOf<HourlyForecast>()

                            for (i in 0 until list.length()) {
                                val item = list.getJSONObject(i)
                                val dt_txt = item.getString("dt_txt")
                                val main = item.getJSONObject("main")
                                val clouds = item.getJSONObject("clouds")
                                val wind = item.getJSONObject("wind")
                                val rain = item.optJSONObject("rain")?.optDouble("1h", 0.0) ?: 0.0

                                val forecast = HourlyForecast(
                                    dt_txt = dt_txt,
                                    temperature = main.getDouble("temp"),
                                    seaLevel = main.optDouble("sea_level", 0.0),
                                    cloudiness = clouds.getInt("all"),
                                    windSpeed = wind.getDouble("speed"),
                                    rainVolume = rain
                                )

                                forecastList.add(forecast)
                            }

                            callback(forecastList)
                        } catch (e: Exception) {
                            logError("DronePilotApp", "OpenWeatherManager: Errore nel parsing dei dati meteo", e)
                            callback(null)
                        }
                    } else {
                        callback(null)
                    }
                }
            }
        })
    }
}

data class HourlyForecast(
    val dt_txt: String,
    val temperature: Double,
    val seaLevel: Double,
    val cloudiness: Int,
    val windSpeed: Double,
    val rainVolume: Double
)
