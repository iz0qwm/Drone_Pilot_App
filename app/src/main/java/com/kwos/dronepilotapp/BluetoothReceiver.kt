package com.kwos.dronepilotapp

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.kwos.dronepilotapp.droneid.OpenDroneIdDataManager
import com.kwos.dronepilotapp.data.LogMessageEntry
import java.util.Locale

class BluetoothReceiver(
    private val context: Context,
    private val dataManager: OpenDroneIdDataManager
) {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                val scanRecord = it.scanRecord
                val bytes = scanRecord?.bytes ?: return

                val logMessageEntry = LogMessageEntry()

                val transportType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    bluetoothAdapter?.isLeCodedPhySupported() == true &&
                    it.primaryPhy == BluetoothDevice.PHY_LE_CODED
                ) {
                    "BT5"
                } else {
                    "BT4"
                }

                dataManager.receiveDataBluetooth(bytes, it, logMessageEntry, transportType)

                val logStr = String.format(
                    Locale.US,
                    "scan: addr=%s rssi=%d, len=%d",
                    it.device.address,
                    it.rssi,
                    bytes.size
                )
                //Log.w("DronePilotApp", logStr)
                //Log.w("DronePilotApp", "BluetoothReceiver -- bytes: ${dumpBytes(bytes)}")
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

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeScanner?.startScan(scanCallback)
            Log.d("DronePilotApp", "BluetoothReceiver: Bluetooth LE scan started")
        } else {
            Log.w("DronePilotApp", "BluetoothReceiver: Missing BLUETOOTH_SCAN permission")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopScanning() {
        bluetoothLeScanner?.stopScan(scanCallback)
        Log.d("DronePilotApp", "BluetoothReceiver: Bluetooth LE scan stopped")
    }

    private fun dumpBytes(bytes: ByteArray): String {
        return bytes.joinToString(" ") { String.format("%02X", it) }
    }
}
