package com.kwos.dronepilotapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat

class BluetoothScanService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DronePilotApp::BluetoothScanWakeLock")
        wakeLock.acquire()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            (application as DronePilotApp).bluetoothReceiver?.startScanning()
        } else {
            Log.w("DronePilotApp", "BluetoothScanService: Permesso BLUETOOTH_SCAN mancante")
        }


        startForeground(2, createNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            (application as DronePilotApp).bluetoothReceiver?.stopScanning()
        }

        if (wakeLock.isHeld) wakeLock.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "bluetooth_scan_channel"
        val channelName = "Bluetooth Drone Scan"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Drone ID Bluetooth attivo")
            .setContentText("Scansione BLE attiva")
            .setSmallIcon(R.drawable.ic_zones)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
