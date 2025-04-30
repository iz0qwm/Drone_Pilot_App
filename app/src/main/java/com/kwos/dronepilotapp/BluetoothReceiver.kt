package com.kwos.dronepilotapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.kwos.dronepilotapp.data.LogMessageEntry
import com.kwos.dronepilotapp.data.LogWriter
import com.kwos.dronepilotapp.droneid.OpenDroneIdDataManager
import java.util.*

class BluetoothReceiver(
    private val context: Context,
    private val dataManager: OpenDroneIdDataManager,
    private val logger: LogWriter? = null
) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())
    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val macAddress = it.device.address.uppercase(Locale.US)

                // ATTENZIONE FUNZIONA SOLO CON I DRONETAG
                val allowedPrefixes = listOf("D0:EA:26")

                if (allowedPrefixes.none { macAddress.startsWith(it) }) {
                    return
                }


                val scanRecord = it.scanRecord
                val bytes = scanRecord?.bytes ?: return

                val logMessageEntry = LogMessageEntry()
                val timeNano = SystemClock.elapsedRealtimeNanos()

                val transportType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    bluetoothAdapter?.isLeCodedPhySupported() == true &&
                    it.primaryPhy == BluetoothDevice.PHY_LE_CODED
                ) {
                    "BT5"
                } else {
                    "BT4"
                }

                dataManager.receiveDataBluetooth(bytes, it, logMessageEntry, transportType)

                logger?.logBluetooth(
                    logMessageEntry.msgVersion,
                    it,
                    transportType,
                    logMessageEntry.messageLogEntry
                )

                //Log.d("DronePilotApp", String.format(
                //    Locale.US,
                //    "✅ BluetoothReceiver: Pacchetto valido da MAC=%s rssi=%d len=%d",
                //    macAddress, it.rssi, bytes.size
                //))
            }

        }

        override fun onScanFailed(errorCode: Int) {
            Log.e("DronePilotApp", "BluetoothReceiver: Bluetooth scan failed: $errorCode")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun startScanning() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)

        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val isDroneIdEnabled = prefs.getBoolean("droneIdEnabled", false)

        if (!isDroneIdEnabled) {
            Log.d("DronePilotApp", "BluetoothReceiver: Rilevamento disattivato, scan non avviato")
            return
        }

        if (bluetoothAdapter?.isEnabled != true) {
            Log.w("DronePilotApp", "BluetoothReceiver: Bluetooth non abilitato")
            return
        }

        if (permissionCheck == PackageManager.PERMISSION_GRANTED && !isScanning) {
            bluetoothLeScanner?.startScan(scanCallback)
            isScanning = true
            Log.d("DronePilotApp", "BluetoothReceiver: Bluetooth LE scan started")

            handler.postDelayed(scanRestartRunnable, 10000) // ogni 10 secondi
        } else {
            Log.w("DronePilotApp", "BluetoothReceiver: Missing BLUETOOTH_SCAN permission or already scanning")
        }
    }

    private val scanRestartRunnable = object : Runnable {
        override fun run() {
            if (isScanning) {
                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)

                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    bluetoothLeScanner?.stopScan(scanCallback)
                    bluetoothLeScanner?.startScan(scanCallback)
                    Log.d("DronePilotApp", "BluetoothReceiver: Bluetooth LE scan restarted")
                } else {
                    Log.w("DronePilotApp", "BluetoothReceiver: Permission BLUETOOTH_SCAN non concessa al riavvio scan")
                }

                handler.postDelayed(this, 10000)
            }
        }

    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        if (isScanning) {
            bluetoothLeScanner?.stopScan(scanCallback)
            handler.removeCallbacks(scanRestartRunnable)
            isScanning = false
            Log.d("DronePilotApp", "BluetoothReceiver: Bluetooth LE scan stopped")
        }
    }

    private fun dumpBytes(bytes: ByteArray): String {
        return bytes.joinToString(" ") { String.format("%02X", it) }
    }
}
