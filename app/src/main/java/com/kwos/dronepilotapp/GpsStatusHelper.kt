package com.kwos.dronepilotapp

import android.annotation.SuppressLint
import android.content.Context
import android.location.GnssStatus
import android.location.LocationManager
import android.os.Bundle
import java.util.concurrent.Executors

class GpsStatusHelper(
    context: Context,
    private val callback: (totalSatellites: Int, usedSatellites: Int) -> Unit
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val executor = Executors.newSingleThreadExecutor()

    private val gnssStatusCallback = object : GnssStatus.Callback() {
        override fun onSatelliteStatusChanged(status: GnssStatus) {
            //logDebug("DronePilotApp", "onSatelliteStatusChanged CHIAMATO: Satelliti totali=${status.satelliteCount}")
            val totalSatellites = status.satelliteCount
            var usedSatellites = 0

            for (i in 0 until totalSatellites) {
                if (status.usedInFix(i)) {
                    usedSatellites++
                }
            }
            //logDebug("DronePilotApp", "onSatelliteStatusChanged: Satelliti totali: $totalSatellites, Usati per il fix: $usedSatellites")
            callback(totalSatellites, usedSatellites)
        }
    }

    @SuppressLint("MissingPermission")
    fun startListening() {
        try {
            //logDebug("DronePilotApp", "GpsStatusHelper:  Registrazione callback GNSS in corso...")
            locationManager.registerGnssStatusCallback(executor, gnssStatusCallback)
            //logDebug("DronePilotApp", "GpsStatusHelper: Callback GNSS registrato con successo!")
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            //logDebug("DronePilotApp", "startListening: GPS attivo: $isGpsEnabled")

            // Richiede un aggiornamento della posizione
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L, // intervallo in millisecondi
                1f,    // distanza minima in metri
                object : android.location.LocationListener {
                    override fun onLocationChanged(location: android.location.Location) {
                        //logDebug(
                        //    "DronePilotApp",
                        //    "Posizione aggiornata: lat=${location.latitude}, lon=${location.longitude}"
                        //)
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
