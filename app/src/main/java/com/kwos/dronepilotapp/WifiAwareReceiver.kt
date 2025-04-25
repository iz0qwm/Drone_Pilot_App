package com.kwos.dronepilotapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.aware.*
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.kwos.dronepilotapp.data.LogMessageEntry
import com.kwos.dronepilotapp.data.LogWriter
import com.kwos.dronepilotapp.droneid.OpenDroneIdDataManager
import java.util.*

class WifiAwareReceiver(
    private val context: Context,
    private val dataManager: OpenDroneIdDataManager,
    private val logger: LogWriter? = null
) {

    private var wifiAwareManager: WifiAwareManager? = null
    private var wifiAwareSession: WifiAwareSession? = null
    private var isSupported = false

    companion object {
        private const val TAG = "DronePilotApp"
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE)
        ) {
            isSupported = true
            wifiAwareManager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as WifiAwareManager

            val filter = IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED)
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(c: Context?, i: Intent?) {
                    if (wifiAwareManager?.isAvailable == true) {
                        Log.i(TAG, "WiFi Aware disponibile (broadcast)")
                        startSession()
                    } else {
                        Toast.makeText(context, "WiFi Aware non disponibile", Toast.LENGTH_SHORT).show()
                    }
                }
            }, filter)
        }
    }

    @SuppressLint("MissingPermission")
    fun startSession() {
        if (!isSupported || wifiAwareManager?.isAvailable != true) return

        try {
            wifiAwareManager?.attach(object : AttachCallback() {
                override fun onAttached(session: WifiAwareSession) {
                    wifiAwareSession = session
                    val config = SubscribeConfig.Builder()
                        .setServiceName("org.opendroneid.remoteid")
                        .build()

                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.NEARBY_WIFI_DEVICES
                        ) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Log.e(TAG, "Permessi mancanti per il WiFi Aware")
                        return
                    }

                    session.subscribe(config, object : DiscoverySessionCallback() {
                        override fun onSubscribeStarted(session: SubscribeDiscoverySession) {
                            Log.i(TAG, "SubscribeDiscoverySession avviata")
                        }

                        override fun onServiceDiscovered(
                            peerHandle: PeerHandle,
                            serviceSpecificInfo: ByteArray,
                            matchFilter: MutableList<ByteArray>?
                        ) {
                            Log.i(TAG, "Servizio scoperto: ${Arrays.toString(serviceSpecificInfo)}")

                            val logEntry = LogMessageEntry()
                            val timestamp = SystemClock.elapsedRealtimeNanos()
                            val hash = peerHandle.hashCode()
                            val transport = "NAN"

                            try {
                                dataManager.receiveDataNaN(
                                    serviceSpecificInfo,
                                    hash,
                                    timestamp,
                                    logEntry,
                                    transport
                                )

                                logger?.logNaN(
                                    logEntry.msgVersion,
                                    timestamp,
                                    hash,
                                    serviceSpecificInfo,
                                    transport,
                                    logEntry.messageLogEntry
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Errore parsing dati WiFi Aware: ${e.message}")
                            }
                        }
                    }, null)
                }

                override fun onAttachFailed() {
                    Toast.makeText(context, "Attach a WiFi Aware fallito", Toast.LENGTH_SHORT).show()
                }
            }, object : IdentityChangedListener() {
                override fun onIdentityChanged(mac: ByteArray) {
                    Log.i(TAG, "Identità cambiata: ${mac.joinToString(":") { "%02x".format(it) }}")
                }
            }, null)
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException su WiFi Aware attach: ${e.message}")
        }
    }

    fun stopSession() {
        wifiAwareSession?.close()
        wifiAwareSession = null
        Log.i(TAG, "Sessione WiFi Aware chiusa")
    }
}
