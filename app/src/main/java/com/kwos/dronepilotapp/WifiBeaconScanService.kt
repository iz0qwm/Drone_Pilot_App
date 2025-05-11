package com.kwos.dronepilotapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat

class WifiBeaconScanService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DronePilotApp::WifiBeaconWakeLock")
        wakeLock.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Avvia il receiver già creato nella DashboardActivity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED) {
                (application as DronePilotApp).wifiBeaconReceiver?.startScan()
            } else {
                Log.w("DronePilotApp", "Permesso NEARBY_WIFI_DEVICES non concesso")
            }
        } else {
            (application as DronePilotApp).wifiBeaconReceiver?.startScan()
        }

        startForeground(1, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        (application as? DronePilotApp)?.wifiBeaconReceiver?.stopScan()
        Log.d("DronePilotApp", "WifiBeaconScanService: stopScan() chiamato")

        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "wifi_scan_channel"
        val channelName = "WiFi Drone Scan"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Drone ID WiFi attivo")
            .setContentText("Scansione WiFi attiva")
            .setSmallIcon(R.drawable.ic_zones)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
