package com.kwos.dronepilotapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import org.json.JSONObject
import kotlin.math.*

class AircraftLayer(private val context: Context, private val map: GoogleMap) {

    private val aircraftMarkers = mutableMapOf<String, Marker>()
    private val aircraftTrailsPolylines = mutableMapOf<String, Polyline>()
    private val aircraftTrails = mutableMapOf<String, MutableList<LatLng>>()
    private var handler: Handler? = null
    private var isFetching = false
    private var backoffUntil = 0L
    private var useAnonymousFetch = false

    fun start() {
        if (handler != null) return
        fetchAircraft()
        handler = Handler(Looper.getMainLooper()).apply {
            postDelayed(fetchRunnable, 60_000)
        }
    }

    fun stop() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
        clearMarkers()
    }

    private val fetchRunnable = object : Runnable {
        override fun run() {
            fetchAircraft()
            handler?.postDelayed(this, 60_000)
        }
    }

    private fun fetchAircraft() {
        val now = System.currentTimeMillis()
        if (now < backoffUntil || isFetching) return
        isFetching = true

        val bounds = map.projection.visibleRegion.latLngBounds
        val lamin = max(36.0, bounds.southwest.latitude)
        val lamax = min(47.5, bounds.northeast.latitude)
        val lomin = max(6.0, bounds.southwest.longitude)
        val lomax = min(19.0, bounds.northeast.longitude)

        if (lamin >= lamax || lomin >= lomax) {
            isFetching = false
            return
        }

        val url = "https://opensky-network.org/api/states/all?lamin=$lamin&lomin=$lomin&lamax=$lamax&lomax=$lomax"

        val request = object : JsonObjectRequest(Method.GET, url, null,
            { response ->
                handleResponse(response)
                isFetching = false
            },
            { error ->
                if (error.networkResponse?.statusCode == 429) {
                    useAnonymousFetch = true
                    backoffUntil = System.currentTimeMillis() + 2 * 60 * 1000
                }
                Log.e("AircraftLayer", "Errore: ${error.message}")
                isFetching = false
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                return if (useAnonymousFetch) {
                    mutableMapOf()
                } else {
                    val creds = "raffaello.dimartino:RaDa0707"
                    val auth = "Basic " + Base64.encodeToString(creds.toByteArray(), Base64.NO_WRAP)
                    mutableMapOf("Authorization" to auth)
                }
            }
        }

        Volley.newRequestQueue(context).add(request)
    }

    private fun handleResponse(response: JSONObject) {
        val states = response.optJSONArray("states") ?: return
        val seen = mutableSetOf<String>()
        val nowSec = System.currentTimeMillis() / 1000

        for (i in 0 until states.length()) {
            val state = states.optJSONArray(i) ?: continue
            val icao = state.optString(0)
            val callsign = state.optString(1).trim()
            val lon = state.optDouble(5, Double.NaN)
            val lat = state.optDouble(6, Double.NaN)
            val geoAlt = state.optDouble(13, Double.NaN)
            val speed = state.optDouble(9, Double.NaN)
            val heading = state.optDouble(10, Double.NaN)
            val category = state.optInt(17, -1)
            val timePosition = state.optLong(3, 0L)
            val secondsAgo = if (timePosition > 0) nowSec - timePosition else -1

            if (!lat.isNaN() && !lon.isNaN()) {
                val isHelicopter = category == 6 || callsign.startsWith("POLI") || callsign.startsWith("PS") || callsign.startsWith("CC")
                val isLowFlying = isHelicopter || (!geoAlt.isNaN() && geoAlt <= 1000)

                if (isLowFlying) {
                    seen.add(icao)
                    val pos = LatLng(lat, lon)

                    if (aircraftMarkers.containsKey(icao)) {
                        aircraftMarkers[icao]?.let { animateMarkerMove(it, pos, heading.toFloat()) }
                        updateTrail(icao, pos)
                    } else {
                        val iconResId = if (isHelicopter) R.drawable.ic_helicopter_map else R.drawable.ic_airplane_map
                        val icon = getVectorBitmapDescriptor(context, iconResId)

                        val snippet = """
                            Altitudine: ${if (!geoAlt.isNaN()) "${geoAlt.roundToInt()} m" else "?"}
                            Velocità: ${if (!speed.isNaN()) "${(speed * 3.6).roundToInt()} km/h" else "?"}
                            Direzione: ${if (!heading.isNaN()) "${heading.roundToInt()}°" else "?"}
                            Agg.: ${if (secondsAgo >= 0) "$secondsAgo s fa" else "?"}
                        """.trimIndent()


                        val marker = map.addMarker(
                            MarkerOptions()
                                .position(pos)
                                .icon(icon)
                                .anchor(0.5f, 0.5f)
                                .rotation(if (!heading.isNaN()) heading.toFloat() else 0f)
                                .flat(true)
                                .title(callsign)
                                .snippet(snippet)
                        )

                        if (marker != null) {
                            aircraftMarkers[icao] = marker
                            updateTrail(icao, pos)
                        }
                    }
                }
            }
        }

        // Rimuovi aerei scomparsi
        val toRemove = aircraftMarkers.keys - seen
        for (icao in toRemove) {
            aircraftMarkers[icao]?.remove()
            aircraftMarkers.remove(icao)

            aircraftTrails[icao]?.clear()
            aircraftTrails.remove(icao)

            aircraftTrailsPolylines[icao]?.remove()
            aircraftTrailsPolylines.remove(icao)
        }

        // InfoWindow HTML (una sola volta)
        map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            override fun getInfoContents(marker: Marker): View {
                val view = LayoutInflater.from(context).inflate(R.layout.infowindow_aircraft, null)

                val titleView = view.findViewById<TextView>(R.id.aircraftCallsign)
                val detailsView = view.findViewById<TextView>(R.id.aircraftDetails)

                titleView.text = marker.title
                detailsView.text = marker.snippet

                return view
            }

            override fun getInfoWindow(marker: Marker): View? = null
        })

    }


    private fun updateTrail(icao: String, pos: LatLng) {
        val trail = aircraftTrails.getOrPut(icao) { mutableListOf() }
        trail.add(pos)
        if (trail.size > 10) trail.removeAt(0)

        val polyline = aircraftTrailsPolylines.getOrPut(icao) {
            map.addPolyline(
                PolylineOptions()
                    .color(0xFF800080.toInt()) // Viola scuro
                    .width(3f)
                    .geodesic(true)
            )
        }

        polyline.points = trail
    }

    private fun clearMarkers() {
        for (marker in aircraftMarkers.values) {
            marker.remove()
        }
        aircraftMarkers.clear()

        for (polyline in aircraftTrailsPolylines.values) {
            polyline.remove()
        }
        aircraftTrailsPolylines.clear()
        aircraftTrails.clear()
    }


    fun getVectorBitmapDescriptor(context: Context, drawableId: Int): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return BitmapDescriptorFactory.defaultMarker()
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    companion object {
        const val helicopterPath = "M 0 -10 L 2 -5 L 4 0 L 4 5 L -4 5 L -4 0 L -2 -5 Z"
        const val airplanePath = "M -10 -2 L -10 -3 L 0 -4 L 0 -9 L 1 -11 L 4 -11 L 5 -9 L 5 -4 L 15 -3 L 15 -2 L 5 0 L 5 4 L 8 6 L 8 8 L 5 6 L 5 10 L 0 10 L 0 6 L -3 8 L -3 6 L 0 4 L 0 0 Z"
    }

    private fun animateMarkerMove(marker: Marker, to: LatLng, heading: Float) {
        val start = marker.position
        val handler = Handler(Looper.getMainLooper())
        val startTime = System.currentTimeMillis()
        val duration = 1000L // 1 secondo

        handler.post(object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - startTime
                val t = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                val lat = start.latitude + t * (to.latitude - start.latitude)
                val lng = start.longitude + t * (to.longitude - start.longitude)

                marker.position = LatLng(lat, lng)
                marker.rotation = heading

                if (t < 1f) {
                    handler.postDelayed(this, 16) // ~60fps
                }
            }
        })
    }

}
