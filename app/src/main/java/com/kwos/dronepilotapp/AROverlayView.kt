package com.kwos.dronepilotapp

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.google.android.gms.maps.model.LatLng
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import com.kwos.dronepilotapp.POI

class AROverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    //private var poiList: List<Pair<String, LatLng>> = emptyList()
    private var poiList: List<POI> = emptyList()
    private var deviceLocation: LatLng = LatLng(0.0, 0.0)
    private var azimuth: Float = 0f
    private var azimuthOffset: Float = 0f
    private var pitch: Float = 0f
    private var roll: Float = 0f
    private val paint = Paint().apply {
        color = Color.WHITE
        textSize = 56f
        isAntiAlias = true
    }
    private val azimuthHistory = ArrayDeque<Float>()
    private val pitchHistory = ArrayDeque<Float>()
    private val smoothingWindow = 12
    private var maxVisibleDistanceMeters: Double = 5000.0

    private var noFlyZones: List<NoFlyZone> = emptyList()
    private var remotePilots: List<RemotePilot> = emptyList()


    fun setPOIs(pois: List<POI>, userLocation: LatLng) {
        poiList = pois
        deviceLocation = userLocation
        invalidate()
    }

    fun setAzimuthOffset(offsetDegrees: Float) {
        azimuthOffset = offsetDegrees
    }

    fun setOrientation(azimuth: Float, pitch: Float, roll: Float) {
        azimuthHistory.add(azimuth)
        pitchHistory.add(pitch)

        if (azimuthHistory.size > smoothingWindow) azimuthHistory.removeFirst()
        if (pitchHistory.size > smoothingWindow) pitchHistory.removeFirst()

        val avgAzimuth = azimuthHistory.average().toFloat()
        val avgPitch = pitchHistory.average().toFloat()

        this.azimuth = (avgAzimuth + azimuthOffset + 360f) % 360f
        this.pitch = avgPitch
        this.roll = roll
        invalidate()
    }

    fun setRemotePilots(pilots: List<RemotePilot>) {
        remotePilots = pilots
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //val maxDistance = 10000.0 // metri
        val maxDistance = maxVisibleDistanceMeters
        val horizontalFOV = 60.0 // gradi visibili orizzontalmente (approssimato)
        val posText = "📍 Tu sei a Lat: %.5f  Lon: %.5f".format(deviceLocation.latitude, deviceLocation.longitude)
        val textWidth = paint.measureText(posText)
        val topMargin = resources.displayMetrics.density * 24  // ~16dp
        canvas.drawText(posText, (width - textWidth) / 2f, topMargin, paint)
        val usedYPositions = mutableListOf<Float>()
        val verticalSpacing = 60f  // spazio minimo tra etichette

        poiList.forEach { poi ->
            val name = poi.name
            val latLng = LatLng(poi.latitude, poi.longitude)
            val distance = haversine(deviceLocation.latitude, deviceLocation.longitude, latLng.latitude, latLng.longitude)
            val bearing = bearingBetweenLocations(deviceLocation, latLng)
            val relativeBearing = normalizeAngle(bearing - azimuth)
            val elev = elevationAngle(deviceLocation.latitude, deviceLocation.longitude, latLng.latitude, latLng.longitude, pitch)

            if (distance > maxDistance) return@forEach
            if (abs(relativeBearing) > horizontalFOV / 2) return@forEach
            if (elev < -30f || elev > 45f) return@forEach

            val screenX = width / 2 + (width / horizontalFOV * relativeBearing).toFloat()
            val screenY = height / 2f - elev * 5f

            // evita overlap
            val tooClose = usedYPositions.any { abs(it - screenY) < verticalSpacing }
            if (tooClose) return@forEach
            usedYPositions.add(screenY)


            val emoji = when (poi.type) {
                "town", "village", "hamlet" -> "🏘️ "
                "historic" -> "🏛️ "
                "viewpoint", "natural" -> "🌄 "
                "attraction" -> "📍 "
                else -> "📌 "
            }

            // scrittura Nome POI e distanza
            val nameText = "$emoji$name"
            val distanceText = "${distance.toInt()} m"
            val padding = 16f
            val spacing = 12f
            val textHeight = paint.textSize
            val boxWidth = maxOf(paint.measureText(nameText), paint.measureText(distanceText)) + padding * 2
            val boxHeight = textHeight * 2 + spacing + padding * 2

            val boxLeft = screenX - boxWidth / 2
            val boxTop = screenY - padding
            val boxRight = screenX + boxWidth / 2
            val boxBottom = boxTop + boxHeight

            // Sfondo semi-trasparente
            val bgPaint = Paint().apply {
                color = Color.parseColor("#66000000") // Nero 40% opaco
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 12f, 12f, bgPaint)

            val borderPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 12f, 12f, borderPaint)


            // Calcola posizione centrata per ciascuna riga
            val nameX = boxLeft + (boxWidth - paint.measureText(nameText)) / 2
            val nameY = boxTop + padding + textHeight
            val distanceX = boxLeft + (boxWidth - paint.measureText(distanceText)) / 2
            val distanceY = nameY + spacing + textHeight

            canvas.drawText(nameText, nameX, nameY, paint)
            canvas.drawText(distanceText, distanceX, distanceY, paint)

        }

        var zoneIndex = 0  // 🔢 contatore per distanziare le zone verticalmente

        noFlyZones.forEach { zone ->

            val distance = haversine(deviceLocation.latitude, deviceLocation.longitude, zone.center.latitude, zone.center.longitude)
            val bearing = bearingBetweenLocations(deviceLocation, zone.center)
            val relativeBearing = normalizeAngle(bearing - azimuth)
            val elev = elevationAngle(deviceLocation.latitude, deviceLocation.longitude, zone.center.latitude, zone.center.longitude, pitch)

            if (distance > maxVisibleDistanceMeters) return@forEach
            if (abs(relativeBearing) > horizontalFOV / 2) return@forEach
            if (elev < -30f || elev > 45f) return@forEach

            val screenX = width / 2 + (width / horizontalFOV * relativeBearing).toFloat()

            // 🔼 Offset verticale progressivo per evitare sovrapposizione
            val screenY = height / 2f - elev * 5f - 150f - (zoneIndex * 150f)
            zoneIndex++

            val warningText = "🚫 ${zone.name}"
            val altText = "Max ${zone.lowerLimit} m a ${distance.toInt()} m"
            val padding = 16f
            val spacing = 12f
            val textHeight = paint.textSize
            val boxWidth = maxOf(paint.measureText(warningText), paint.measureText(altText)) + padding * 2
            val boxHeight = textHeight * 2 + spacing + padding * 2

            val boxLeft = screenX - boxWidth / 2
            val boxTop = screenY - padding
            val boxRight = screenX + boxWidth / 2
            val boxBottom = boxTop + boxHeight

            val bgColor = ZoneColorUtils.getColorForLowerLimit(zone.lowerLimit) ?: return@forEach
            val bgPaint = Paint().apply {
                color = bgColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 12f, 12f, bgPaint)

            val warningX = boxLeft + (boxWidth - paint.measureText(warningText)) / 2
            val warningY = boxTop + padding + textHeight
            val altX = boxLeft + (boxWidth - paint.measureText(altText)) / 2
            val altY = warningY + spacing + textHeight

            canvas.drawText(warningText, warningX, warningY, paint)
            canvas.drawText(altText, altX, altY, paint)
            // Log.d("AROverlay", "✅ Disegno zona ${zone.name} in AR")
        }

        remotePilots.forEach { pilot ->
            val latLng = LatLng(pilot.latitude, pilot.longitude)
            val distance = haversine(deviceLocation.latitude, deviceLocation.longitude, latLng.latitude, latLng.longitude)
            val bearing = bearingBetweenLocations(deviceLocation, latLng)
            val relativeBearing = normalizeAngle(bearing - azimuth)
            val elev = elevationAngle(deviceLocation.latitude, deviceLocation.longitude, latLng.latitude, latLng.longitude, pitch)

            if (distance > maxVisibleDistanceMeters) return@forEach
            if (abs(relativeBearing) > horizontalFOV / 2) return@forEach
            if (elev < -30f || elev > 45f) return@forEach

            val screenX = width / 2 + (width / horizontalFOV * relativeBearing).toFloat()
            val screenY = height / 2f - elev * 5f - 80f

            val pilotEmoji = "👨‍✈️"
            val nameText = "$pilotEmoji ${pilot.name} (${pilot.drone})"
            val distText = "${distance.toInt()} m"
            val padding = 16f
            val spacing = 12f
            val textHeight = paint.textSize
            val boxWidth = maxOf(paint.measureText(nameText), paint.measureText(distText)) + padding * 2
            val boxHeight = textHeight * 2 + spacing + padding * 2

            val boxLeft = screenX - boxWidth / 2
            val boxTop = screenY - padding
            val boxRight = screenX + boxWidth / 2
            val boxBottom = boxTop + boxHeight

            val bgPaint = Paint().apply {
                color = Color.parseColor("#5500FF00") // verde trasparente
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 12f, 12f, bgPaint)

            val borderPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }
            canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 12f, 12f, borderPaint)

            val nameX = boxLeft + (boxWidth - paint.measureText(nameText)) / 2
            val nameY = boxTop + padding + textHeight
            val distX = boxLeft + (boxWidth - paint.measureText(distText)) / 2
            val distY = nameY + spacing + textHeight

            canvas.drawText(nameText, nameX, nameY, paint)
            canvas.drawText(distText, distX, distY, paint)
        }


    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setWillNotDraw(false)  // 🔥 forza il sistema a chiamare onDraw()
    }

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // raggio terra
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(
            dLon / 2
        ).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun bearingBetweenLocations(start: LatLng, end: LatLng): Float {
        val lat1 = Math.toRadians(start.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon2 = Math.toRadians(end.longitude)

        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))
        return ((bearing + 360) % 360).toFloat()
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle
        while (a < -180f) a += 360f
        while (a > 180f) a -= 360f
        return a
    }

    private fun elevationAngle(deviceLat: Double, deviceLon: Double, poiLat: Double, poiLon: Double, pitch: Float): Float {
        val dist = haversine(deviceLat, deviceLon, poiLat, poiLon)
        val height = 1.7f  // altezza simulata osservatore (puoi regolare)
        return Math.toDegrees(atan2(height.toDouble(), dist)).toFloat() + pitch
    }

    fun setMaxVisibleDistance(distance: Double) {
        maxVisibleDistanceMeters = distance
        invalidate()
    }

    fun setNoFlyZones(zones: List<NoFlyZone>) {
        noFlyZones = zones
        Log.d("AROverlay", "📍 Ricevute ${zones.size} zone. Invalido ora + tra 1 secondo.")
        invalidate()

        postDelayed({
            invalidate()
        }, 2000)
    }


}