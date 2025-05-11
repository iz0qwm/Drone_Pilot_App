package com.kwos.dronepilotapp

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.util.Log
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class FlightZoneLayer(private val context: Context, private val map: GoogleMap) {

    private val zonePolygons = mutableListOf<Polygon>()
    private val geoJsonUrl = "https://www.kwos.org/appoggio/droni/dflight_geozones.json"

    private fun getColorFromLowerLimit(lowerLimit: Int): Int {
        return when (lowerLimit) {
            0 -> Color.RED
            25 -> Color.parseColor("#FFA500") // arancione
            45 -> Color.YELLOW
            60 -> Color.parseColor("#ADD8E6") // azzurro
            else -> Color.RED
        }
    }

    fun drawZones() {
        val request = Request.Builder().url(geoJsonUrl).build()
        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("DronePilotApp", "FlightZoneLayer: Errore nel caricamento JSON", e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    try {
                        val json = JSONObject(jsonString)
                        val features = json.getJSONArray("features")

                        for (i in 0 until features.length()) {
                            val feature = features.getJSONObject(i)
                            val name = feature.optString("name", "")
                            val geometries = feature.getJSONArray("geometry")

                            for (j in 0 until geometries.length()) {
                                val geo = geometries.getJSONObject(j)
                                val lowerLimit = geo.optInt("lowerLimit", 0)
                                val projection = geo.getJSONObject("horizontalProjection")

                                if (projection.getString("type") == "Polygon") {
                                    val coordinates = projection.getJSONArray("coordinates").getJSONArray(0)
                                    val path = mutableListOf<LatLng>()
                                    for (k in 0 until coordinates.length()) {
                                        val coord = coordinates.getJSONArray(k)
                                        val lng = coord.getDouble(0)
                                        val lat = coord.getDouble(1)
                                        path.add(LatLng(lat, lng))
                                    }

                                    // 👉 Controllo per polygons vuoti
                                    if (path.isEmpty()) {
                                        Log.w("DronePilotApp", "FlightZoneLayer: Poligono ignorato: coordinate vuote.")
                                        continue
                                    }

                                    val fillColor = getColorFromLowerLimit(lowerLimit)

                                    val polygonOptions = PolygonOptions()
                                        .addAll(path)
                                        .strokeColor(fillColor)
                                        .strokeWidth(1f)
                                        .strokePattern(null) // niente pattern per ora
                                        .fillColor(fillColor and 0x44FFFFFF.toInt()) // semitrasparente

                                    if (context is Activity) {
                                        context.runOnUiThread {
                                            val polygon = map.addPolygon(polygonOptions)
                                            zonePolygons.add(polygon)
                                        }
                                    }

                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("DronePilotApp", "FlightZoneLayer: Errore parsing JSON", e)
                    }
                }
            }
        })
    }

    fun clearZones() {
        zonePolygons.forEach { it.remove() }
        zonePolygons.clear()
    }
}
