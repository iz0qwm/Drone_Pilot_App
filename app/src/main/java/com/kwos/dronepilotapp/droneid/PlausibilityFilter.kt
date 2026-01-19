// PlausibilityFilter.kt
package com.kwos.dronepilotapp.droneid

import kotlin.math.*

data class DroneSample(
    val mac: String,
    val lat: Double?,
    val lon: Double?,
    val altitudeM: Double?,   // AMSL o ellissoidale: usa ciò che hai
    val speedMS: Double?,     // m/s
    val tsMillis: Long
)

class PlausibilityFilter(
    private val receiverLat: () -> Double?,   // posizione del telefono / ricevitore
    private val receiverLon: () -> Double?,
    private val maxRangeMeters: Double = 2000.0,
    private val minAltM: Double = -120.0,
    private val maxAltM: Double = 1200.0,
    private val maxSpeedMS: Double = 50.0,    // hard cap
    private val jumpSpeedCapMS: Double = 60.0,// cap su salto tra pacchetti consecutivi
    private val confirmWindowMs: Long = 10_000L,
    private val staleMs: Long = 30_000L
) {
    private data class TrackState(var last: DroneSample? = null, var validHits: Int = 0, var firstSeen: Long = 0)
    private val states = HashMap<String, TrackState>()

    enum class DropReason {
        STALE, NO_LATLON, BAD_RANGE, BAD_LATLON, ZERO_MISMATCH,
        ALT_RANGE, SPD_RANGE, JUMP_TOO_FAST, FIRST_NEEDS_CONFIRM
    }

    var onDrop: ((DroneSample, DropReason) -> Unit)? = null

    fun isPlausible(s: DroneSample): Boolean {
        val now = System.currentTimeMillis()

        // helper locale per loggare i drop
        fun drop(reason: DropReason): Boolean {
            onDrop?.invoke(s, reason)
            return false
        }

        // Scarta pacchetti “vecchi”
        if (now - s.tsMillis > staleMs) return drop(DropReason.STALE)

        // Lat/Lon validi?
        val lat = s.lat ?: return drop(DropReason.NO_LATLON)
        val lon = s.lon ?: return drop(DropReason.NO_LATLON)
        if (lat !in -90.0..90.0 || lon !in -180.0..180.0) return drop(DropReason.BAD_LATLON)
        if ((lat == 0.0 && lon != 0.0) || (lon == 0.0 && lat != 0.0)) return drop(DropReason.ZERO_MISMATCH)

        // Distanza dal ricevitore (solo se noto)
        val rLat = receiverLat()
        val rLon = receiverLon()
        if (rLat != null && rLon != null) {
            val dist = haversineMeters(lat, lon, rLat, rLon)
            if (dist > maxRangeMeters) return drop(DropReason.BAD_RANGE)
        }

        // Quota (opzionale)
        s.altitudeM?.let { alt ->
            if (alt < minAltM || alt > maxAltM) return drop(DropReason.ALT_RANGE)
        }

        // Velocità (opzionale)
        s.speedMS?.let { spd ->
            if (spd < 0 || spd > maxSpeedMS) return drop(DropReason.SPD_RANGE)
        }

        // Coerenza col pacchetto precedente dello stesso MAC
        val st = states.getOrPut(s.mac) { TrackState(firstSeen = now) }
        st.last?.let { prev ->
            val dt = max(1L, s.tsMillis - prev.tsMillis) / 1000.0
            val dMeters = haversineMeters(prev.lat!!, prev.lon!!, lat, lon)
            val impliedV = dMeters / dt
            if (impliedV > jumpSpeedCapMS) return drop(DropReason.JUMP_TOO_FAST)
        }

        // Conferma minima 2x
        if (st.last == null) {
            st.validHits = 1
            st.firstSeen = now
            st.last = s
            return drop(DropReason.FIRST_NEEDS_CONFIRM)
        } else {
            if (now - st.firstSeen <= confirmWindowMs) {
                st.validHits += 1
            } else {
                st.validHits = 1
                st.firstSeen = now
            }
            st.last = s
            return st.validHits >= 2
        }
    }


    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat/2)*sin(dLat/2) + cos(Math.toRadians(lat1))*cos(Math.toRadians(lat2))*sin(dLon/2)*sin(dLon/2)
        return 2 * R * atan2(sqrt(a), sqrt(1 - a))
    }
}
