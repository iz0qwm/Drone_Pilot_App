package com.kwos.dronepilotapp

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.LocationManager
import java.util.concurrent.Executors

class GpsStatusHelper(
    context: Context,
    private val callback: (totalSatellites: Int, usedSatellites: Int) -> Unit
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val executor = Executors.newSingleThreadExecutor()

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            val totalSatellites = status.satelliteCount
            var usedSatellites = 0

            for (i in 0 until totalSatellites) {
                if (status.usedInFix(i)) {
                    usedSatellites++
                }
            }

            callback(totalSatellites, usedSatellites)
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        locationManager.registerGnssStatusCallback(executor, gnssStatusCallback)
    }

    fun stopListening() {
        locationManager.unregisterGnssStatusCallback(gnssStatusCallback)
    }
}
