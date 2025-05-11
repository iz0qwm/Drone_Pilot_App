// DronePilotApp.kt
package com.kwos.dronepilotapp

import android.app.Application

class DronePilotApp : Application() {
    var bluetoothReceiver: BluetoothReceiver? = null
    var wifiBeaconReceiver: WifiBeaconReceiver? = null
}
