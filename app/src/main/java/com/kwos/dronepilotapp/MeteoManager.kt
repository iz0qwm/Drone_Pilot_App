package com.kwos.dronepilotapp

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Response
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.*

data class MeteoData(
    val temperature: Int,
    val temperature_min: Int,
    val temperature_max: Int,
    val wind_speedmean: Int,
    val wind_speedmax: Int,
    val wind_speedmin: Int,
    val humidity: Int,
    val precipitation_probability: Int,
    val convective_precipitation: Int
)



data class TecData(
    val dt: String,
    val refresh_rate: String,
    @SerializedName("tec_mean") val tecMean: String,
    @SerializedName("tec_std") val tecStd: String,
    val jfile: String,
    @SerializedName("tec_med_27_days") val tecMed27Days: String
)

data class ApiResponse(
    val records: List<TecData>
)


object MeteoManager {
    private val TAG = "DronePilotApp"
    private const val API_KEY = "tqz42yKjRbD18S7o" // Sostituisci con la tua API Key
    private const val BASE_URL = "https://my.meteoblue.com/packages/basic-day_webcolors"
    private const val BASE_URL_INGV = "http://ws-eswua.rm.ingv.it/tecdb.php/records/wsnc_eu"


    fun getMeteoData(lat: Double, lon: Double, callback: (MeteoData?) -> Unit) {
        val url = "$BASE_URL?windspeed=kmh&lat=$lat&lon=$lon&tz=Europe/Zurich&format=json&apikey=$API_KEY"
        val urlCurrent = "https://my.meteoblue.com/packages/current?windspeed=kmh&lat=$lat&lon=$lon&tz=Europe/Zurich&format=json&apikey=$API_KEY"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        logDebug(TAG, "MeteoManager URL: $url")
        logDebug(TAG, "MeteoManager URL Current: $urlCurrent")

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()
                logDebug(TAG, "MeteoManager Lunghezza responseData: ${responseData?.length}")
                logDebug(TAG, "MeteoManager Risposta API: $responseData")
                if (!response.isSuccessful || responseData == null) {
                    logError(TAG, "MeteoManager Errore risposta Data")
                    callback(null)
                    return@Thread
                }

                val jsonObject = JSONObject(responseData)
                val dataDay = jsonObject.getJSONObject("data_day")
                val temperatureMin = dataDay.optJSONArray("temperature_min").getDouble(0).toInt()
                val temperatureMax = dataDay.optJSONArray("temperature_max").getDouble(0).toInt()
                val windSpeedmean = dataDay.optJSONArray("windspeed_mean").getDouble(0).toInt()
                val windSpeedmax = dataDay.optJSONArray("windspeed_max").getDouble(0).toInt()
                val windSpeedmin = dataDay.optJSONArray("windspeed_min").getDouble(0).toInt()
                val humidity = dataDay.optJSONArray("relativehumidity_mean").getDouble(0).toInt()
                val precipitationprobability = dataDay.optJSONArray("precipitation_probability").getDouble(0).toInt()
                val convectiveprecipitation = dataDay.optJSONArray("convective_precipitation").getDouble(0).toInt()


                // Seconda richiesta: Current
                val responseCurrent = client.newCall(Request.Builder().url(urlCurrent).build()).execute()
                val responseDataCurrent = responseCurrent.body?.string()
                logDebug(TAG, "MeteoManager Lunghezza responseData: ${responseDataCurrent?.length}")
                logDebug(TAG, "MeteoManager Risposta API: $responseDataCurrent")

                if (!responseCurrent.isSuccessful || responseDataCurrent == null) {
                    logError(TAG, "Errore nella risposta CURRENT")
                    callback(null)
                    return@Thread
                }

                val currentObject = JSONObject(responseDataCurrent)
                val dataCurrent = currentObject.getJSONObject("data_current")
                val temperature = dataCurrent.optDouble("temperature", Double.NaN).toInt()


                logDebug(TAG, "MeteoManager Temperature: $temperature")
                logDebug(TAG, "MeteoManager temperatureMin: $temperatureMin")
                logDebug(TAG, "MeteoManager temperatureMax: $temperatureMax")
                logDebug(TAG, "MeteoManager windSpeedmax: $windSpeedmax")
                logDebug(TAG, "MeteoManager windSpeedmean: $windSpeedmean")
                logDebug(TAG, "MeteoManager windSpeedmin: $windSpeedmin")
                logDebug(TAG, "MeteoManager humidity: $humidity")
                logDebug(TAG, "MeteoManager precipitation_probability: $precipitationprobability")
                logDebug(TAG, "MeteoManager convective precipitation: $convectiveprecipitation")

                val meteoData = MeteoData(temperature, temperatureMin, temperatureMax, windSpeedmean, windSpeedmax, windSpeedmin, humidity, precipitationprobability, convectiveprecipitation)
                callback(meteoData)
            } catch (e: Exception) {
                logError(TAG, "MeteoManager Errore nel parsing JSON: ${e.message}")
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    // Funzione per ottenere i dati TEC (Total Electron Content)
    fun getTecData(callback: (Float?) -> Unit) {
        val url = getFormattedTecUrl()
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        logDebug(TAG, "MeteoManager TEC URL: $url")
        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()
                logDebug(TAG, "MeteoManager Risposta API TEC: $responseData")

                if (!response.isSuccessful || responseData == null) {
                    logError(TAG, "MeteoManager Errore risposta TEC")
                    callback(null)
                    return@Thread
                }

                // Parsing del JSON per ottenere il campo tec_mean
                val jsonObject = JSONObject(responseData)
                val records = jsonObject.getJSONArray("records")
                if (records.length() > 0) {
                    val record = records.getJSONObject(0)
                    val tecMean = record.getDouble("tec_mean").toFloat()
                    logDebug(TAG, "TEC Mean: $tecMean")
                    callback(tecMean)
                } else {
                    callback(null)
                }
            } catch (e: Exception) {
                logError(TAG, "MeteoManager Errore nel parsing JSON TEC: ${e.message}")
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }

    // Funzione per ottenere l'URL formattato per il recupero dei dati TEC
    private fun getFormattedTecUrl(): String {
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val minutes = now.get(Calendar.MINUTE)
        val roundedMinutes = (minutes / 10) * 10
        now.set(Calendar.MINUTE, roundedMinutes)
        now.set(Calendar.SECOND, 0)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val formattedDate = dateFormat.format(now.time)

        return "$BASE_URL_INGV?filter=dt,eq,$formattedDate"
    }


    //Funzione caricamento Meteogram All in One
    fun getMeteogramOneImageUrl(lat: Double, lon: Double): String {
        return "https://my.meteoblue.com/images/meteogram_one?windspeed=kmh&lat=$lat&lon=$lon&tz=Europe/Zurich&apikey=$API_KEY"
    }

    //Funzione caricamento Meteogram All in One
    fun getMeteogramImageUrl(lat: Double, lon: Double): String {
        return "https://my.meteoblue.com/images/meteogram?windspeed=kmh&lat=$lat&lon=$lon&tz=Europe/Zurich&apikey=$API_KEY"
    }

    // ---- NUOVA funzione per l'alba e tramonto ----
    fun getDaylightData(lat: Double, lon: Double, callback: (JSONObject?) -> Unit) {
        val url = "https://api.sunrise-sunset.org/json?lat=$lat&lng=$lon&date=today&tzid=Europe/Rome"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        logDebug(TAG, "MeteoManager URL Alba/Tramonto: $url")

        Thread {
            try {
                val response = client.newCall(request).execute()
                val responseData = response.body?.string()

                if (!response.isSuccessful || responseData == null) {
                    logError(TAG, "Errore nella risposta alba/tramonto")
                    callback(null)
                    return@Thread
                }

                val jsonObject = JSONObject(responseData)
                val results = jsonObject.getJSONObject("results")
                callback(results)

            } catch (e: Exception) {
                logError(TAG, "Errore parsing JSON alba/tramonto: ${e.message}")
                e.printStackTrace()
                callback(null)
            }
        }.start()
    }
}