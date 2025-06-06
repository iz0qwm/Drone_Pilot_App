package com.kwos.dronepilotapp

import android.graphics.Color
import android.util.Log
import com.kwos.dronepilotapp.NoFlyZone
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*

object ZoneColorUtils {

    fun getColorForLowerLimit(limit: Int): Int? {
        return when (limit) {
            0 -> Color.argb(140, 255, 0, 0)       // rosso
            25 -> Color.argb(140, 255, 165, 0)    // arancione
            45 -> Color.argb(140, 255, 255, 0)    // giallo
            60 -> Color.argb(140, 0, 191, 255)    // celeste
            else -> null                          // non disegnare
        }
    }


    fun parseZonesFromJson(jsonString: String): List<NoFlyZone> {
        val zones = mutableListOf<NoFlyZone>()
        val json = org.json.JSONObject(jsonString)
        val features = json.getJSONArray("features")

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val name = feature.optString("name", "Zona")
            val geometries = feature.getJSONArray("geometry")

            for (j in 0 until geometries.length()) {
                val geo = geometries.getJSONObject(j)
                val lowerLimit = geo.optInt("lowerLimit", 0)
                val upperLimit = geo.optInt("upperLimit", 120)
                val projection = geo.getJSONObject("horizontalProjection")

                if (projection.getString("type") == "Polygon") {
                    val coordsArray = projection.getJSONArray("coordinates").getJSONArray(0)

                    var latSum = 0.0
                    var lonSum = 0.0
                    val points = mutableListOf<LatLng>()

                    for (k in 0 until coordsArray.length()) {
                        val coord = coordsArray.getJSONArray(k)
                        val lng = coord.getDouble(0)
                        val lat = coord.getDouble(1)
                        latSum += lat
                        lonSum += lng
                        points.add(LatLng(lat, lng))
                    }

                    if (points.isEmpty()) continue

                    val center = LatLng(latSum / points.size, lonSum / points.size)
                    val radius = points.maxOf {
                        haversine(center.latitude, center.longitude, it.latitude, it.longitude)
                    }

                    val color = getColorForLowerLimit(lowerLimit)
                    if (color != null) {
                        zones.add(NoFlyZone(name, center, radius, lowerLimit, upperLimit, color))
                        //Log.d("ZoneColorUtils", "✔ Aggiunta zona: $name - lowerLimit=$lowerLimit m")
                    } else {
                        Log.d("ZoneColorUtils", "❌ Zona scartata: $name - lowerLimit=$lowerLimit (nessun colore definito)")
                    }
                }
            }
        }

        return zones
    }


    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun distanceBetweenMeters(p1: LatLng, p2: LatLng): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(p2.latitude - p1.latitude)
        val dLon = Math.toRadians(p2.longitude - p1.longitude)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(p1.latitude)) * cos(Math.toRadians(p2.latitude)) * sin(dLon / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

}
