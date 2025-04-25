package com.kwos.dronepilotapp.data

import com.kwos.dronepilotapp.droneid.OpenDroneIdParser
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.BasicId
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.OperatorID
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.SelfID
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.SystemMsg

class LogMessageEntry {

    private val messages = mutableListOf<OpenDroneIdParser.Message<*>>() // non-nullable

    var msgVersion: Int = 0

    fun add(message: OpenDroneIdParser.Message<*>) {
        messages.add(message)
    }

    val messageLogEntry: StringBuilder?
        get() {
            if (messages.isEmpty()) return null

            messages.sort() // funziona se Message implementa Comparable

            val entry = StringBuilder()
            var i = 0

            // 2 BASIC_ID
            repeat(2) {
                if (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.BASIC_ID) {
                    val msg = messages[i] as OpenDroneIdParser.Message<BasicId>
                    entry.append(msg.payload?.toCsvString() ?: DELIM_BASIC_ID)
                    i++
                } else {
                    entry.append(DELIM_BASIC_ID)
                }
            }

            while (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.BASIC_ID) i++

            // LOCATION
            if (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.LOCATION) {
                val msg = messages[i] as OpenDroneIdParser.Message<OpenDroneIdParser.Location>
                entry.append(msg.payload?.toCsvString() ?: DELIM_LOCATION)
                i++
            } else {
                entry.append(DELIM_LOCATION)
            }

            while (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.LOCATION) i++

            // SKIP AUTH for now
            while (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.AUTH) i++

            // SELFID
            if (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.SELFID) {
                val msg = messages[i] as OpenDroneIdParser.Message<SelfID>
                entry.append(msg.payload?.toCsvString() ?: DELIM_SELF_ID)
                i++
            } else {
                entry.append(DELIM_SELF_ID)
            }

            while (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.SELFID) i++

            // SYSTEM
            if (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.SYSTEM) {
                val msg = messages[i] as OpenDroneIdParser.Message<SystemMsg>
                entry.append(msg.payload?.toCsvString() ?: DELIM_SYSTEM)
                i++
            } else {
                entry.append(DELIM_SYSTEM)
            }

            while (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.SYSTEM) i++

            // OPERATOR_ID
            if (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.OPERATOR_ID) {
                val msg = messages[i] as OpenDroneIdParser.Message<OperatorID>
                entry.append(msg.payload?.toCsvString() ?: DELIM_OPERATOR)
            } else {
                entry.append(DELIM_OPERATOR)
            }

            // AUTHENTICATION (fino a MAX_AUTH_DATA_PAGES)
            i = 0
            while (i < messages.size && messages[i].header.type in listOf(
                    OpenDroneIdParser.Type.BASIC_ID,
                    OpenDroneIdParser.Type.LOCATION
                )
            ) i++

            for (j in 0 until Constants.MAX_AUTH_DATA_PAGES) {
                if (i < messages.size && messages[i].header.type == OpenDroneIdParser.Type.AUTH) {
                    val msg =
                        messages[i] as OpenDroneIdParser.Message<OpenDroneIdParser.Authentication>
                    if (msg.payload?.authDataPage == j) {
                        entry.append(msg.payload.toCsvString())
                        i++
                    } else {
                        entry.append(DELIM_AUTHENTICATION)
                    }
                } else {
                    entry.append(DELIM_AUTHENTICATION)
                }
            }

            return entry
        }

    companion object {
        private const val DELIM = Constants.DELIM

        private val DELIM_BASIC_ID = DELIM.repeat(3)
        private val DELIM_LOCATION = DELIM.repeat(21)
        private val DELIM_AUTHENTICATION = DELIM.repeat(6)
        private val DELIM_SELF_ID = DELIM.repeat(2)
        private val DELIM_SYSTEM = DELIM.repeat(12)
        private val DELIM_OPERATOR = DELIM.repeat(2)
    }

}
