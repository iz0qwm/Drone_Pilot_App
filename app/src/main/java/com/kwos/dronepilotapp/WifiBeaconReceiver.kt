package com.kwos.dronepilotapp

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.CountDownTimer
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.kwos.dronepilotapp.data.LogMessageEntry
import com.kwos.dronepilotapp.data.LogWriter
import com.kwos.dronepilotapp.droneid.OpenDroneIdDataManager
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

class WifiBeaconReceiver(
    private val context: Context,
    private val dataManager: OpenDroneIdDataManager,
    private val logger: LogWriter?
) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var scanSuccess = 0
    private var scanFailed = 0
    private val startTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    private var countDownTimer: CountDownTimer? = null
    private val driCid = byteArrayOf(0xFA.toByte(), 0x0B.toByte(), 0xBC.toByte())
    private val vendorTypeValue = 0x0D.toByte()
    private var isReceiverRegistered = false
    private var isScanSupported = true

    private val receiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                //Log.d("DronePilotApp", "WifiBeaconReceiver: SCAN_RESULTS_AVAILABLE received")
                handleScanResults()
            }
        }
    }

    init {
        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI)) {
            Toast.makeText(context, "WiFi not supported", Toast.LENGTH_LONG).show()
            isScanSupported = false
        }

        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (!isReceiverRegistered) {
            context.registerReceiver(receiver, filter)
            isReceiverRegistered = true
            Log.d("DronePilotApp", "WifiBeaconReceiver: Receiver registered")
        }

        if (isScanSupported) {
            startCountDownTimer()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun handleScanResults() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("WifiBeaconReceiver", "Location permission not granted")
            return
        }

        val scanResults = wifiManager.scanResults
        scanResults.forEach { scanResult ->
            try {
                handleResult(scanResult)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        startScan()
    }

    private fun handleResult(scanResult: ScanResult) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val field = ScanResult::class.java.getDeclaredField("informationElements")
            field.isAccessible = true
            val elements = field.get(scanResult) as? Array<Any> ?: return

            for (element in elements) {
                val idField = element.javaClass.getDeclaredField("id")
                val id = idField.getInt(element)
                if (id == 221) {
                    val bytesField = element.javaClass.getDeclaredField("bytes")
                    val bytes = bytesField.get(element) as? ByteArray ?: continue
                    processRemoteIdVendorIE(scanResult, bytes)
                }
            }
        } else {
            scanResult.informationElements?.forEach { element ->
                if (element.id == 221) {
                    val buf = element.bytes
                    val bytes = ByteArray(buf.remaining())
                    buf.get(bytes)
                    processRemoteIdVendorIE(scanResult, bytes)
                }
            }
        }
    }

    private fun processRemoteIdVendorIE(scanResult: ScanResult, raw: ByteArray) {
        if (raw.size < 30) return

        val buf = ByteBuffer.wrap(raw).asReadOnlyBuffer()
        val cid = ByteArray(3).apply { buf.get(this) }
        val type = buf.get()

        if (!cid.contentEquals(driCid) || type != vendorTypeValue) return

        Log.d("DronePilotApp", "✅ WifiBeaconReceiver: Pacchetto DJI OpenDroneID valido da BSSID=${scanResult.BSSID}")

        buf.position(4)
        val payload = ByteArray(buf.remaining()).apply { buf.get(this) }

        val timeNano = SystemClock.elapsedRealtimeNanos()
        val logMessageEntry = LogMessageEntry()

        dataManager.receiveDataWiFiBeacon(
            payload, scanResult.BSSID, scanResult.BSSID.hashCode().toLong(),
            scanResult.level, timeNano, logMessageEntry, "Beacon"
        )

        logger?.logBeacon(logMessageEntry.msgVersion, timeNano, scanResult, payload, "Beacon", logMessageEntry.messageLogEntry)
    }



    fun startScan() {
        if (!isScanSupported) return

        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val isDroneIdEnabled = prefs.getBoolean("droneIdEnabled", false)

        if (!isDroneIdEnabled) {
            //Log.d("DronePilotApp", "WifiBeaconReceiver: Rilevamento disattivato, scan non avviato")
            return
        }

        if (countDownTimer != null) {
            Log.d("DronePilotApp", "WifiBeaconReceiver: Scan già in corso")
            return
        }

        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 10000) { // ogni 10 secondi
            override fun onTick(millisUntilFinished: Long) {
                val success = wifiManager.startScan()
                if (success) {
                    Log.d("DronePilotApp", "WifiBeaconReceiver: scan avviato")
                } else {
                    Log.w("DronePilotApp", "WifiBeaconReceiver: scan fallito")
                }
            }

            override fun onFinish() {
                // Non dovrebbe mai finire in realtà
            }
        }.start()

        Log.d("DronePilotApp", "WifiBeaconReceiver: CountDownTimer avviato")

        // ✅ REGISTRAZIONE SICURA del broadcast receiver
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            context.registerReceiver(receiver, intentFilter)
            isReceiverRegistered = true
            Log.d("DronePilotApp", "WifiBeaconReceiver: Broadcast receiver registrato")
        }

    }


    fun stopScan() {
        countDownTimer?.cancel()
        countDownTimer = null

        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
                isReceiverRegistered = false
                Log.d("DronePilotApp", "WifiBeaconReceiver: Receiver unregistered")
            } catch (e: IllegalArgumentException) {
                Log.w("DronePilotApp", "WifiBeaconReceiver: Receiver not registered: ${e.message}")
            }
        }
    }


    private fun startCountDownTimer() {
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 2000) {
            override fun onTick(millisUntilFinished: Long) {
                startScan()
            }

            override fun onFinish() {}
        }.start()
    }
}