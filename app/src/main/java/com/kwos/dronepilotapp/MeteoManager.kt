package com.kwos.dronepilotapp

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class MeteoData(
    val temperature_min: Double,
    val temperature_max: Double,
    val wind_speedmean: Double,
    val wind_speedmax: Double,
    val wind_speedmin: Double,
    val humidity: Double
)


object MeteoManager {
    private val TAG = "DronePilotApp"
    private const val API_KEY = "tqz42yKjRbD18S7o" // Sostituisci con la tua API Key
    private const val BASE_URL = "https://my.meteoblue.com/packages/basic-day_webcolors"

    fun getMeteoData(lat: Double, lon: Double, callback: (MeteoData?) -> Unit) {
        val url = "$BASE_URL?lat=$lat&lon=$lon&tz=Europe/Zurich&format=json&apikey=$API_KEY"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        logDebug(TAG, "MeteoManager URL: $url")
        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()
                logDebug(TAG, "MeteoManager Lunghezza responseData: ${responseData?.length}")
                logDebug(TAG, "MeteoManager Risposta API: $responseData")
                if (!response.isSuccessful || responseData == null) {
                    logError(TAG, "MeteoManager Errore risposta")
                    callback(null)
                    return@Thread
                }

                val jsonObject = JSONObject(responseData)
                val dataDay = jsonObject.getJSONObject("data_day")
                val temperatureMin = dataDay.optJSONArray("temperature_min").getDouble(0)
                val temperatureMax = dataDay.optJSONArray("temperature_max").getDouble(dataDay.optJSONArray("temperature_max").length() - 1)
                val windSpeedmean = dataDay.optJSONArray("windspeed_mean").getDouble(dataDay.optJSONArray("windspeed_mean").length() - 1)
                val windSpeedmax = dataDay.optJSONArray("windspeed_max").getDouble(dataDay.optJSONArray("windspeed_max").length() - 1)
                val windSpeedmin = dataDay.optJSONArray("windspeed_min").getDouble(dataDay.optJSONArray("windspeed_min").length() - 1)
                val humidity = dataDay.optJSONArray("relativehumidity_mean").getDouble(dataDay.optJSONArray("relativehumidity_mean").length() - 1)

                logDebug(TAG, "MeteoManager temperatureMin: $temperatureMin")
                logDebug(TAG, "MeteoManager temperatureMax: $temperatureMax")
                logDebug(TAG, "MeteoManager windSpeedmax: $windSpeedmax")
                logDebug(TAG, "MeteoManager windSpeedmean: $windSpeedmean")
                logDebug(TAG, "MeteoManager windSpeedmin: $windSpeedmin")
                logDebug(TAG, "MeteoManager humidity: $humidity")

                val meteoData = MeteoData(temperatureMin, temperatureMax, windSpeedmean, windSpeedmax, windSpeedmin, humidity)
                callback(meteoData)
            } catch (e: Exception) {
                logError(TAG, "MeteoManager Errore nel parsing JSON: ${e.message}")
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    fun getMeteogramImageUrl(lat: Double, lon: Double): String {
        return "https://my.meteoblue.com/images/meteogram_one?lat=$lat&lon=$lon&tz=Europe/Zurich&apikey=$API_KEY"
    }
}
