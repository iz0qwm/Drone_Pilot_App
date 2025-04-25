package com.kwos.dronepilotapp.data

import java.util.Locale


class Connection : MessageData() {
    var rssi: Int = 0
    var transportType: String? = null
    var macAddress: String? = null
    var lastSeen: Long = 0
    var firstSeen: Long = 0
    var msgDelta: Long = 0

    val msgDeltaAsString: String
        get() {
            if (msgDelta / 1000 == 0L) return String.format(Locale.US, "%3d ms", msgDelta)
            else {
                var seconds = msgDelta.toDouble()
                seconds /= 1000.0
                return String.format(Locale.US, "%.1f s", seconds)
            }
        }
}
