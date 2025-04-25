package com.kwos.dronepilotapp.droneid


import android.util.Log
import com.kwos.dronepilotapp.data.Constants
import com.kwos.dronepilotapp.data.LogMessageEntry
import java.nio.ByteBuffer
import java.nio.ByteOrder

object OpenDroneIdParser {
    private const val TAG = "DronePilotApp"
    private val DELIM: String = Constants.DELIM

    private const val LAT_LONG_MULTIPLIER = 1e-7
    private const val SPEED_VERTICAL_MULTIPLIER = 0.5

    fun parseData(
        payload: ByteArray, offset: Int, timestamp: Long,
        logMessageEntry: LogMessageEntry,
        receiverLocation: android.location.Location?
    ): Message<Payload?>? {
        if (offset <= 0 || payload.size < offset + Constants.MAX_MESSAGE_SIZE) return null

        val msgCounter = payload[offset - 1].toInt() and 0xFF
        return parseMessage(
            payload,
            offset,
            timestamp,
            logMessageEntry,
            receiverLocation,
            msgCounter
        )
    }

    fun parseMessage(
        payload: ByteArray, offset: Int, timestamp: Long,
        logMessageEntry: LogMessageEntry,
        receiverLocation: android.location.Location?, msgCounter: Int
    ): Message<Payload?>? {
        if (payload.size < offset + Constants.MAX_MESSAGE_SIZE) return null

        val byteBuffer = ByteBuffer.wrap(payload, offset, Constants.MAX_MESSAGE_SIZE)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val header = Header()
        val b = byteBuffer.get().toInt() and 0xFF
        val type = (b and 0xF0) shr 4
        header.type = Type.Companion.fromId(type)
        if (header.type == null) {
            Log.e(TAG, "OpenDroneIdParser: Header type unknown")
            return null
        }
        header.version = b and 0x0F

        var payloadObj: Payload? = null

        when (header.type) {
            Type.BASIC_ID -> payloadObj = parseBasicId(byteBuffer)
            Type.LOCATION -> payloadObj = parseLocation(byteBuffer, receiverLocation)
            Type.AUTH -> payloadObj = parseAuthentication(byteBuffer)
            Type.SELFID -> payloadObj = parseSelfID(byteBuffer)
            Type.SYSTEM -> payloadObj = parseSystem(byteBuffer)
            Type.OPERATOR_ID -> payloadObj = parseOperatorID(byteBuffer)
            Type.MESSAGE_PACK -> payloadObj = parseMessagePack(payload, offset)
            else -> Log.w(TAG, "OpenDroneIdParser: Received unhandled message type: id=" + type)

        }
        val message = Message<Payload?>(header, payloadObj, timestamp, msgCounter)
        logMessageEntry.msgVersion = message.header.version

        if (header.type != Type.MESSAGE_PACK) logMessageEntry.add(message)
        return message
    }

    private fun parseBasicId(byteBuffer: ByteBuffer): BasicId {
        val basicId = BasicId()

        val type = byteBuffer.get().toInt()
        basicId.idType = (type and 0xF0) shr 4
        basicId.uaType = type and 0x0F
        byteBuffer.get(basicId.uasId, 0, Constants.MAX_ID_BYTE_SIZE)
        return basicId
    }

    private fun parseLocation(
        byteBuffer: ByteBuffer,
        receiverLocation: android.location.Location?
    ): Location {
        val location = Location()

        val b = byteBuffer.get().toInt()
        location.status = (b and 0xF0) shr 4
        location.heightType = (b and 0x04) shr 2
        location.EWDirection = (b and 0x02) shr 1
        location.speedMult = b and 0x01

        location.Direction = byteBuffer.get().toInt() and 0xFF
        location.speedHori = byteBuffer.get().toInt() and 0xFF
        location.speedVert = byteBuffer.get().toInt()

        location.droneLat = byteBuffer.getInt()
        location.droneLon = byteBuffer.getInt()

        location.altitudePressure = byteBuffer.getShort().toInt() and 0xFFFF
        location.altitudeGeodetic = byteBuffer.getShort().toInt() and 0xFFFF
        location.height = byteBuffer.getShort().toInt() and 0xFFFF

        val horiVertAccuracy = byteBuffer.get().toInt()
        location.horizontalAccuracy = horiVertAccuracy and 0x0F
        location.verticalAccuracy = (horiVertAccuracy and 0xF0) shr 4
        val speedBaroAccuracy = byteBuffer.get().toInt()
        location.baroAccuracy = (speedBaroAccuracy and 0xF0) shr 4
        location.speedAccuracy = speedBaroAccuracy and 0x0F
        location.timestamp = byteBuffer.getShort().toInt() and 0xFFFF
        location.timeAccuracy = byteBuffer.get().toInt() and 0x0F

        // Use an older retrieved receiver location to calculate the distance to the drone
        if (location.droneLat != 0 && location.droneLon != 0) {
            val droneLoc = android.location.Location("")
            droneLoc.setLatitude(location.latitude)
            droneLoc.setLongitude(location.longitude)
            if (receiverLocation != null) location.distance = receiverLocation.distanceTo(droneLoc)
        }

        return location
    }

    private fun parseAuthentication(byteBuffer: ByteBuffer): Authentication {
        val authentication = Authentication()

        val type = byteBuffer.get().toInt()
        authentication.authType = (type and 0xF0) shr 4
        authentication.authDataPage = type and 0x0F

        var offset = 0
        var amount: Int = Constants.MAX_AUTH_PAGE_ZERO_SIZE
        if (authentication.authDataPage == 0) {
            authentication.authLastPageIndex = byteBuffer.get().toInt() and 0xFF
            authentication.authLength = byteBuffer.get().toInt() and 0xFF
            authentication.authTimestamp = byteBuffer.getInt().toLong() and 0xFFFFFFFFL

            // For an explanation, please see the description for struct ODID_Auth_data in:
            // https://github.com/opendroneid/opendroneid-core-c/blob/master/libopendroneid/opendroneid.h
            val len: Int =
                authentication.authLastPageIndex * Constants.MAX_AUTH_PAGE_NON_ZERO_SIZE +
                        Constants.MAX_AUTH_PAGE_ZERO_SIZE
            if (authentication.authLastPageIndex >= Constants.MAX_AUTH_DATA_PAGES ||
                authentication.authLength > len
            ) {
                authentication.authLastPageIndex = 0
                authentication.authLength = 0
                authentication.authTimestamp = 0
            } else {
                // Display both normal authentication data and any possible additional data
                authentication.authLength = len
            }
        } else {
            offset = Constants.MAX_AUTH_PAGE_ZERO_SIZE +
                    (authentication.authDataPage - 1) * Constants.MAX_AUTH_PAGE_NON_ZERO_SIZE
            amount = Constants.MAX_AUTH_PAGE_NON_ZERO_SIZE
        }
        if (authentication.authDataPage >= 0 && authentication.authDataPage < Constants.MAX_AUTH_DATA_PAGES) for (i in offset..<offset + amount) authentication.authData[i] =
            byteBuffer.get()
        return authentication
    }

    private fun parseSelfID(byteBuffer: ByteBuffer): SelfID {
        val selfID = SelfID()
        selfID.descriptionType = byteBuffer.get().toInt() and 0xFF
        byteBuffer.get(selfID.operationDescription, 0, Constants.MAX_STRING_BYTE_SIZE)
        return selfID
    }

    private fun parseSystem(byteBuffer: ByteBuffer): SystemMsg {
        val s = SystemMsg()

        var b = byteBuffer.get().toInt()
        s.operatorLocationType = b and 0x03
        s.classificationType = (b and 0x1C) shr 2
        s.operatorLatitude = byteBuffer.getInt()
        s.operatorLongitude = byteBuffer.getInt()
        s.areaCount = byteBuffer.getShort().toInt() and 0xFFFF
        s.areaRadius = byteBuffer.get().toInt() and 0xFF
        s.areaCeiling = byteBuffer.getShort().toInt() and 0xFFFF
        s.areaFloor = byteBuffer.getShort().toInt() and 0xFFFF
        b = byteBuffer.get().toInt()
        s.category = (b and 0xF0) shr 4
        s.classValue = b and 0x0F
        s.operatorAltitudeGeo = byteBuffer.getShort().toInt() and 0xFFFF
        s.systemTimestamp = byteBuffer.getInt().toLong() and 0xFFFFFFFFL
        return s
    }

    private fun parseOperatorID(byteBuffer: ByteBuffer): OperatorID {
        val operatorID = OperatorID()
        operatorID.operatorIdType = byteBuffer.get().toInt() and 0xFF
        byteBuffer.get(operatorID.operatorId, 0, Constants.MAX_ID_BYTE_SIZE)
        return operatorID
    }


    private fun parseMessagePack(payload: ByteArray, offset: Int): MessagePack? {
        var byteBuffer = ByteBuffer.wrap(payload, offset + 1, 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val messagePack = MessagePack()
        messagePack.messageSize = byteBuffer.get().toInt() and 0xFF
        messagePack.messagesInPack = byteBuffer.get().toInt() and 0xFF

        if (messagePack.messageSize != Constants.MAX_MESSAGE_SIZE || messagePack.messagesInPack <= 0 || messagePack.messagesInPack > Constants.MAX_MESSAGES_IN_PACK || payload.size < offset + 1 + 2 + messagePack.messageSize * messagePack.messagesInPack) return null

        // Now that we know how much data is in the message, re-wrap and extract the data
        byteBuffer = ByteBuffer.wrap(
            payload,
            offset + 1 + 2,
            messagePack.messageSize * messagePack.messagesInPack
        )
        byteBuffer.get(
            messagePack.messages,
            0,
            messagePack.messageSize * messagePack.messagesInPack
        )
        return messagePack
    }

    enum class Type(val id: Int) {
        BASIC_ID(0),
        LOCATION(1),
        AUTH(2),
        SELFID(3),
        SYSTEM(4),
        OPERATOR_ID(5),
        MESSAGE_PACK(0xF),
        UNKNOWN(-1); // valore di fallback per quando il type è null o non riconosciuto

        companion object {
            fun fromId(id: Int): Type? {
                if (id == Type.BASIC_ID.id) {
                    return Type.BASIC_ID
                } else if (id == Type.LOCATION.id) {
                    return Type.LOCATION
                } else if (id == Type.AUTH.id) {
                    return Type.AUTH
                } else if (id == Type.SELFID.id) {
                    return Type.SELFID
                } else if (id == Type.SYSTEM.id) {
                    return Type.SYSTEM
                } else if (id == Type.OPERATOR_ID.id) {
                    return Type.OPERATOR_ID
                } else if (id == Type.MESSAGE_PACK.id) {
                    return Type.MESSAGE_PACK
                } else {
                    return null
                }
            }
        }
    }

    class Header {
        var type: Type? = null
        var version: Int = 0

        override fun toString(): String {
            return "Header{" +
                    "type=" + type +
                    ", version=" + version +
                    '}'
        }
    }

    interface Payload {
        fun toCsvString(): String?
    }

    class BasicId : Payload {
        var idType: Int = 0
        var uaType: Int = 0
        val uasId: ByteArray = ByteArray(Constants.MAX_ID_BYTE_SIZE)

        override fun toCsvString(): String {
            return (idType.toString() + DELIM
                    + uaType + DELIM
                    + String(uasId) + DELIM)
        }

        override fun toString(): String {
            return "BasicId{" +
                    "idType=" + idType +
                    ", uaType=" + uaType +
                    ", uasId='" + uasId.contentToString() + '\'' +
                    '}'
        }

        companion object {
            fun csvHeader(): String {
                return ("idType" + DELIM
                        + "uaType" + DELIM
                        + "uasId" + DELIM)
            }
        }
    }

    class Location : Payload {
        var status: Int = 0
        var heightType: Int = 0
        var EWDirection: Int = 0
        var speedMult: Int = 0
        var Direction: Int = 0
        var speedHori: Int = 0
        var speedVert: Int = 0
        var droneLat: Int = 0
        var droneLon: Int = 0
        var altitudePressure: Int = 0
        var altitudeGeodetic: Int = 0
        var height: Int = 0
        var horizontalAccuracy: Int = 0
        var verticalAccuracy: Int = 0
        var baroAccuracy: Int = 0
        var speedAccuracy: Int = 0
        var timestamp: Int = 0
        var timeAccuracy: Int = 0
        var distance: Float = 0f

        fun getDirection(): Double {
            return calcDirection(Direction, EWDirection)
        }

        fun getSpeedHori(): Double {
            return calcSpeed(speedHori, speedMult)
        }

        fun getSpeedVert(): Double {
            return SPEED_VERTICAL_MULTIPLIER * speedVert
        }

        val latitude: Double
            get() = LAT_LONG_MULTIPLIER * droneLat
        val longitude: Double
            get() = LAT_LONG_MULTIPLIER * droneLon

        fun getAltitudePressure(): Double {
            return calcAltitude(altitudePressure)
        }

        fun getAltitudeGeodetic(): Double {
            return calcAltitude(altitudeGeodetic)
        }

        fun getHeight(): Double {
            return calcAltitude(height)
        }

        fun getTimeAccuracy(): Double {
            return timeAccuracy * 0.1
        }

        override fun toCsvString(): String {
            return (status.toString() + DELIM
                    + heightType + DELIM
                    + EWDirection + DELIM
                    + speedMult + DELIM
                    + Direction + DELIM
                    + speedHori + DELIM
                    + speedVert + DELIM
                    + droneLat + DELIM
                    + droneLon + DELIM
                    + altitudePressure + DELIM
                    + altitudeGeodetic + DELIM
                    + height + DELIM
                    + horizontalAccuracy + DELIM
                    + verticalAccuracy + DELIM
                    + baroAccuracy + DELIM
                    + speedAccuracy + DELIM
                    + timestamp + DELIM
                    + timeAccuracy + DELIM
                    + distance + DELIM)
        }

        override fun toString(): String {
            return "Location{" +
                    "status=" + status +
                    ", heightType=" + heightType +
                    ", EWDirection=" + EWDirection +
                    ", speedMult=" + speedMult +
                    ", direction=" + Direction +
                    ", speedHori=" + speedHori +
                    ", speedVert=" + speedVert +
                    ", droneLat=" + droneLat +
                    ", droneLon=" + droneLon +
                    ", altitudePressure=" + altitudePressure +
                    ", altitudeGeodetic=" + altitudeGeodetic +
                    ", height=" + height +
                    ", horizontalAccuracy=" + horizontalAccuracy +
                    ", verticalAccuracy=" + verticalAccuracy +
                    ", baroAccuracy=" + baroAccuracy +
                    ", speedAccuracy=" + speedAccuracy +
                    ", timestamp=" + timestamp +
                    ", timeAccuracy=" + timeAccuracy +
                    ", distance=" + distance +
                    '}'
        }

        companion object {
            fun calcSpeed(value: Int, mult: Int): Double {
                if (mult == 0) return value * 0.25
                else return (value * 0.75) + (255 * 0.25)
            }

            fun calcDirection(value: Int, EW: Int): Double {
                if (EW == 0) return value.toDouble()
                else return (value + 180).toDouble()
            }

            fun calcAltitude(value: Int): Double {
                return value.toDouble() / 2 - 1000
            }

            fun csvHeader(): String {
                return ("status" + DELIM
                        + "heightType" + DELIM
                        + "EWDirection" + DELIM
                        + "speedMult" + DELIM
                        + "direction" + DELIM
                        + "speedHori" + DELIM
                        + "speedVert" + DELIM
                        + "droneLat" + DELIM
                        + "droneLon" + DELIM
                        + "altitudePressure" + DELIM
                        + "altitudeGeodetic" + DELIM
                        + "height" + DELIM
                        + "horizontalAccuracy" + DELIM
                        + "verticalAccuracy" + DELIM
                        + "baroAccuracy" + DELIM
                        + "speedAccuracy" + DELIM
                        + "timestamp" + DELIM
                        + "timeAccuracy" + DELIM
                        + "distance" + DELIM)
            }
        }
    }

    class Authentication : Payload {
        var authType: Int = 0
        var authDataPage: Int = 0
        var authLastPageIndex: Int = 0
        var authLength: Int = 0
        var authTimestamp: Long = 0
        val authData: ByteArray = ByteArray(Constants.MAX_AUTH_DATA)

        private fun authDataToString(): String {
            val sb = StringBuilder()
            for (authDatum in authData) {
                sb.append(String.format("%02X ", authDatum))
            }
            return sb.toString()
        }

        override fun toCsvString(): String {
            return (authType.toString() + DELIM
                    + authDataPage + DELIM
                    + authLastPageIndex + DELIM
                    + authLength + DELIM
                    + authTimestamp + DELIM
                    + authDataToString() + DELIM)
        }

        override fun toString(): String {
            return "Authentication{" +
                    "authType=" + authType +
                    ", authDataPage=" + authDataPage +
                    ", authLastPageIndex=" + authLastPageIndex +
                    ", authLength=" + authLength +
                    ", authTimestamp=" + authTimestamp +
                    ", authData='" + authData.contentToString() + '\'' +
                    '}'
        }

        companion object {
            fun csvHeader(): String {
                return ("authType" + DELIM
                        + "authDataPage" + DELIM
                        + "authLastPageIndex" + DELIM
                        + "authLength" + DELIM
                        + "authTimestamp" + DELIM
                        + "authData" + DELIM)
            }
        }
    }

    class SelfID : Payload {
        var descriptionType: Int = 0
        val operationDescription: ByteArray = ByteArray(Constants.MAX_STRING_BYTE_SIZE)

        override fun toCsvString(): String {
            return (descriptionType.toString() + DELIM
                    + String(operationDescription) + DELIM)
        }

        override fun toString(): String {
            return "SelfID{" +
                    "descriptionType=" + descriptionType +
                    ", operationDescription='" + operationDescription.contentToString() + '\'' +
                    '}'
        }

        companion object {
            fun csvHeader(): String {
                return ("descriptionType" + DELIM
                        + "operationDescription" + DELIM)
            }
        }
    }

    class SystemMsg : Payload {
        var operatorLocationType: Int = 0
        var classificationType: Int = 0
        var operatorLatitude: Int = 0
        var operatorLongitude: Int = 0
        var areaCount: Int = 0
        var areaRadius: Int = 0
        var areaCeiling: Int = 0
        var areaFloor: Int = 0
        var category: Int = 0
        var classValue: Int = 0
        var operatorAltitudeGeo: Int = 0
        var systemTimestamp: Long = 0

        val latitude: Double
            get() = LAT_LONG_MULTIPLIER * operatorLatitude
        val longitude: Double
            get() = LAT_LONG_MULTIPLIER * operatorLongitude

        val computedAreaRadius: Int
            get() = areaRadius * 10

        val computedAreaCeiling: Double
            get() = calcAltitude(areaCeiling)

        val computedAreaFloor: Double
            get() = calcAltitude(areaFloor)

        val computedOperatorAltitudeGeo: Double
            get() = calcAltitude(operatorAltitudeGeo)

        override fun toCsvString(): String {
            return (operatorLocationType.toString() + DELIM
                    + classificationType + DELIM
                    + operatorLatitude + DELIM
                    + operatorLongitude + DELIM
                    + areaCount + DELIM
                    + areaRadius + DELIM
                    + areaCeiling + DELIM
                    + areaFloor + DELIM
                    + category + DELIM
                    + classValue + DELIM
                    + operatorAltitudeGeo + DELIM
                    + systemTimestamp + DELIM)
        }

        override fun toString(): String {
            return "PilotLocation{" +
                    "operatorLocationType=" + operatorLocationType +
                    ", classificationType=" + classificationType +
                    ", operatorLatitude=" + operatorLatitude +
                    ", operatorLongitude=" + operatorLongitude +
                    ", areaCount=" + areaCount +
                    ", areaRadius=" + areaRadius +
                    ", areaCeiling=" + areaCeiling +
                    ", areaFloor=" + areaFloor +
                    ", category=" + category +
                    ", class=" + classValue +
                    ", operatorAltitudeGeo=" + operatorAltitudeGeo +
                    ", systemTimestamp=" + systemTimestamp +
                    '}'
        }

        companion object {
            fun calcAltitude(value: Int): Double {
                return value.toDouble() / 2 - 1000
            }

            fun csvHeader(): String {
                return ("operatorLocationType" + DELIM
                        + "classificationType" + DELIM
                        + "operatorLatitude" + DELIM
                        + "operatorLongitude" + DELIM
                        + "areaCount" + DELIM
                        + "areaRadius" + DELIM
                        + "areaCeiling" + DELIM
                        + "areaFloor" + DELIM
                        + "category" + DELIM
                        + "classValue" + DELIM
                        + "operatorAltitudeGeo" + DELIM
                        + "systemTimestamp" + DELIM)
            }
        }
    }


    class OperatorID : Payload {
        var operatorIdType: Int = 0
        val operatorId: ByteArray = ByteArray(Constants.MAX_ID_BYTE_SIZE)

        override fun toCsvString(): String {
            return (operatorIdType.toString() + DELIM
                    + String(operatorId) + DELIM)
        }

        override fun toString(): String {
            return "OperatorID{" +
                    "operatorIdType=" + operatorIdType +
                    ", operatorId='" + operatorId.contentToString() + '\'' +
                    '}'
        }

        companion object {
            fun csvHeader(): String {
                return ("operatorIdType" + DELIM
                        + "operatorId" + DELIM)
            }
        }
    }

    class MessagePack : Payload {
        var messageSize: Int = 0
        var messagesInPack: Int = 0
        val messages: ByteArray = ByteArray(Constants.MAX_MESSAGE_PACK_SIZE)

        override fun toString(): String {
            return "MessagePack{" +
                    "messageSize=" + messageSize +
                    ", messagesInPack=" + messagesInPack +
                    ", messages='" + messages.contentToString() + '\'' +
                    '}'
        }

        override fun toCsvString(): String? {
            return null
        }
    }

    class Message<T : Payload?>(
        val header: Header,
        val payload: T?,
        val timestamp: Long,
        val msgCounter: Int
    ) : Comparable<Message<*>> {
        override fun compareTo(o: Message<*>): Int {
            return if (this.header.type == Type.AUTH && o.header.type == Type.AUTH) {
                val authThis = this.payload as Authentication
                val authO = o.payload as Authentication
                authThis.authDataPage - authO.authDataPage
            } else {
                (this.header.type ?: Type.UNKNOWN).compareTo(o.header.type ?: Type.UNKNOWN)

            }
        }
    }

}
