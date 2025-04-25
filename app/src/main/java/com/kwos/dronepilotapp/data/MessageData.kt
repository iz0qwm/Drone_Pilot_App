package com.kwos.dronepilotapp.data

import android.os.SystemClock
import java.sql.Timestamp
import java.util.Locale

open class MessageData {
    var msgCounter: Int = 0
    var timestamp: Long = 0
    var msgVersion: Int = 0

    val msgCounterAsString: String
        get() = String.format(Locale.US, "%3d", msgCounter)

    val timestampAsString: String
        get() {
            val msSinceEvent = (SystemClock.elapsedRealtimeNanos() - timestamp) / 1_000_000L
            val actualTime = System.currentTimeMillis() - msSinceEvent
            val time = Timestamp(actualTime)
            return time.toString()
        }

    val msgVersionAsString: String
        get() = String.format(Locale.US, "v.%d", msgVersion)

    fun msgVersionUnsupported(): Boolean = msgVersion > Constants.MAX_MSG_VERSION
}
