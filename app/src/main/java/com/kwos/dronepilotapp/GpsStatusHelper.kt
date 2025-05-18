package com.kwos.dronepilotapp

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import java.util.concurrent.Executors

class GpsStatusHelper(
    context: Context,
    private val callback: (totalSatellites: Int, usedSatellites: Int, accuracy: Float) -> Unit
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val executor = Executors.newSingleThreadExecutor()

    // Variabili per mantenere i dati più recenti
    private var lastTotalSatellites: Int = 0
    private var lastUsedSatellites: Int = 0

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            lastTotalSatellites = status.satelliteCount
            lastUsedSatellites = 0

            for (i in 0 until status.satelliteCount) {
                if (status.usedInFix(i)) {
                    lastUsedSatellites++
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        try {
            locationManager.registerGnssStatusCallback(executor, gnssStatusCallback)

            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // ogni secondo
                1f,    // ogni metro
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        callback(lastTotalSatellites, lastUsedSatellites, location.accuracy)
                    }

                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
            )

        } catch (e: Exception) {
            logDebug("DronePilotApp", "GpsStatusHelper: Errore nella registrazione del callback GNSS: ${e.message}")
        }
    }

    fun stopListening() {
        locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
    }
}
