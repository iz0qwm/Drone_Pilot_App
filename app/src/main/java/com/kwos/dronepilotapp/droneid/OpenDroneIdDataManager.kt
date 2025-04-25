package com.kwos.dronepilotapp.droneid

import android.bluetooth.le.ScanResult
import android.util.Log
import com.kwos.dronepilotapp.data.*
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.Message
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.BasicId
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.Location
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.Authentication
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.SelfID
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.SystemMsg
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.OperatorID
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.MessagePack
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.Type
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.parseData
import com.kwos.dronepilotapp.droneid.OpenDroneIdParser.parseMessage
import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap

class OpenDroneIdDataManager(private val callback: Callback) {

    val aircraft = ConcurrentHashMap<Long, AircraftObject>()
    var lastKnownDeviceLocation: android.location.Location? = null

    interface Callback {
        fun onNewAircraft(obj: AircraftObject)
        fun onLocationUpdate(obj: AircraftObject)
    }

    fun receiveDataBluetooth(
        data: ByteArray, result: ScanResult,
        logMessageEntry: LogMessageEntry, transportType: String?
    ) {
        val macAddress = result.device.address.replace(":", "")
        val macAddressLong = macAddress.toLong(16)

        val message = parseData(data, 6, result.timestampNanos, logMessageEntry, lastKnownDeviceLocation) ?: return

        receiveData(
            result.timestampNanos, macAddress, macAddressLong, result.rssi,
            message, logMessageEntry, transportType
        )
    }

    fun receiveDataNaN(
        data: ByteArray, peerHash: Int, timeNano: Long,
        logMessageEntry: LogMessageEntry, transportType: String?
    ) {
        val message = parseData(data, 1, timeNano, logMessageEntry, lastKnownDeviceLocation) ?: return

        receiveData(
            timeNano, "NaN ID: $peerHash", peerHash.toLong(), 0,
            message, logMessageEntry, transportType
        )
    }

    fun receiveDataWiFiBeacon(
        data: ByteArray, mac: String, macLong: Long, rssi: Int,
        timeNano: Long, logMessageEntry: LogMessageEntry, transportType: String?
    ) {
        val message = parseData(data, 1, timeNano, logMessageEntry, lastKnownDeviceLocation) ?: return

        receiveData(timeNano, mac, macLong, rssi, message, logMessageEntry, transportType)
    }

    private fun receiveData(
        timeNano: Long, macAddress: String, macAddressLong: Long, rssi: Int,
        message: Message<*>, logMessageEntry: LogMessageEntry, transportType: String?
    ) {
        val currentTime = System.currentTimeMillis()
        val ac = aircraft.getOrPut(macAddressLong) {
            val newAc = createNewAircraft(macAddress, macAddressLong)
            callback.onNewAircraft(newAc)
            newAc
        }

        ac.connection.value?.apply {
            msgDelta = currentTime - lastSeen
            lastSeen = currentTime
            this.rssi = rssi
            this.transportType = transportType
            this.timestamp = timeNano
            this.msgVersion = message.header.version
        }

        ac.connection.postValue(ac.connection.value)

        if (message.header.type == Type.MESSAGE_PACK) {
            handleMessagePack(
                ac, message as Message<MessagePack>, timeNano,
                logMessageEntry, message.msgCounter
            )
        } else {
            handleMessages(ac, message)
        }

        logMessageEntry.msgVersion = ac.connection.value?.msgVersion ?: 0
    }

    private fun createNewAircraft(macAddress: String, macAddressLong: Long): AircraftObject {
        return AircraftObject(macAddressLong).apply {
            connection.value = Connection().apply {
                firstSeen = System.currentTimeMillis()
                this.macAddress = macAddress
            }
            identification1.value = Identification()
            identification2.value = Identification()
            location.value = LocationData()
            authentication.value = AuthenticationData()
            selfid.value = SelfIdData()
            system.value = SystemData()
            operatorid.value = OperatorIdData()
        }
    }

    private fun handleMessages(ac: AircraftObject, message: Message<*>) {
        when (message.header.type) {
            Type.BASIC_ID -> handleBasicId(ac, message as Message<BasicId>)
            Type.LOCATION -> handleLocation(ac, message as Message<Location>)
            Type.AUTH -> handleAuthentication(ac, message as Message<Authentication>)
            Type.SELFID -> handleSelfID(ac, message as Message<SelfID>)
            Type.SYSTEM -> handleSystem(ac, message as Message<SystemMsg>)
            Type.OPERATOR_ID -> handleOperatorID(ac, message as Message<OperatorID>)
            else -> Log.w(TAG, "OpenDroneIdManager: Unhandled message type: ${message.header.type}")
        }
    }

    private fun handleLocation(ac: AircraftObject, message: Message<Location>) {
        val raw = message.payload ?: return
        val data = LocationData().apply {
            msgCounter = message.msgCounter
            timestamp = message.timestamp
            status = LocationData.StatusEnum.fromInt(raw.status)
            heightType = if (raw.heightType == 1) LocationData.HeightTypeEnum.Ground else LocationData.HeightTypeEnum.Takeoff
            direction = if (raw.getDirection() < 0 || raw.getDirection() > 360) 361.0 else raw.getDirection()
            speedHorizontal = if (raw.getSpeedHori() < 0 || raw.getSpeedHori() > 254.25) 255.0 else raw.getSpeedHori()
            speedVertical = if (raw.getSpeedVert() < -62 || raw.getSpeedVert() > 62) 63.0 else raw.getSpeedVert()
            latitude = raw.latitude
            longitude = raw.longitude
            altitudePressure = raw.getAltitudePressure()
            altitudeGeodetic = raw.getAltitudeGeodetic()
            height = raw.getHeight()
            horizontalAccuracy = LocationData.HorizontalAccuracyEnum.values().getOrElse(raw.horizontalAccuracy) { LocationData.HorizontalAccuracyEnum.Unknown }
            verticalAccuracy = LocationData.VerticalAccuracyEnum.values().getOrElse(raw.verticalAccuracy) { LocationData.VerticalAccuracyEnum.Unknown }
            baroAccuracy = LocationData.VerticalAccuracyEnum.values().getOrElse(raw.baroAccuracy) { LocationData.VerticalAccuracyEnum.Unknown }
            speedAccuracy = LocationData.SpeedAccuracyEnum.values().getOrElse(raw.speedAccuracy) { LocationData.SpeedAccuracyEnum.Unknown }
            locationTimestamp = raw.timestamp.toDouble()
            timeAccuracy = raw.getTimeAccuracy()
            distance = raw.distance
        }

        ac.location.value = data
        callback.onLocationUpdate(ac)

        Log.d(TAG, "OpenDroneIdManager: 📡 Location ricevuta: lat=${raw.latitude}, lon=${raw.longitude}, alt=${raw.getAltitudeGeodetic()}, speedH=${raw.getSpeedHori()} rssi=${ac.connection.value?.rssi}")
    }

    private fun handleBasicId(ac: AircraftObject, message: Message<BasicId>) {
        val raw = message.payload ?: return

        val data = Identification().apply {
            msgCounter = message.msgCounter
            timestamp = message.timestamp
            setUaType(raw.uaType)
            setIdType(raw.idType)
            setUasId(raw.uasId)
        }

        val id1 = ac.identification1.value
        val id2 = ac.identification2.value
        if (id1 == null || id2 == null) return

        val type1 = id1.idType
        val type2 = id2.idType

        when {
            type1 == Identification.IdTypeEnum.None || type1 == data.idType -> ac.identification1.value = data
            type2 == Identification.IdTypeEnum.None || type2 == data.idType -> ac.identification2.value = data
            else -> Log.i(TAG, "OpenDroneIdManager: Discarded Basic ID: ${data.idType} (already have $type1 and $type2)")
        }
    }

    private fun handleAuthentication(ac: AircraftObject, message: Message<Authentication>) {
        val raw = message.payload ?: return

        val data = AuthenticationData().apply {
            msgCounter = message.msgCounter
            timestamp = message.timestamp
            setAuthType(raw.authType) // Questo rimane, perché fa conversione enum
            authDataPage = raw.authDataPage
            if (raw.authDataPage == 0) {
                authLastPageIndex = raw.authLastPageIndex
                authLength = raw.authLength
                authTimestamp = raw.authTimestamp
            }
            authData = raw.authData
        }


        ac.authentication.value = ac.combineAuthentication(data)
    }

    private fun handleSelfID(ac: AircraftObject, message: Message<SelfID>) {
        val raw = message.payload ?: return
        val data = SelfIdData().apply {
            msgCounter = message.msgCounter
            timestamp = message.timestamp
            setDescriptionType(raw.descriptionType)
            setOperationDescription(raw.operationDescription)
        }
        ac.selfid.value = data
    }


    private fun handleSystem(ac: AircraftObject, message: Message<SystemMsg>) {
        val raw = message.payload ?: return

        val data = SystemData().apply {
            msgCounter = message.msgCounter
            timestamp = message.timestamp
            setOperatorLocationType(raw.operatorLocationType)
            setClassificationType(raw.classificationType)
            setOperatorLatitude(raw.latitude)
            setOperatorLongitude(raw.longitude)
            areaCount = raw.areaCount
            areaRadius = raw.computedAreaRadius
            areaCeiling = raw.computedAreaCeiling
            areaFloor = raw.computedAreaFloor
            setCategory(raw.category)
            setClassValue(raw.classValue)
            operatorAltitudeGeo = raw.computedOperatorAltitudeGeo
            systemTimestamp = raw.systemTimestamp
        }

        ac.system.value = data
    }

    private fun handleOperatorID(ac: AircraftObject, message: Message<OperatorID>) {
        val raw = message.payload ?: return

        val data = OperatorIdData().apply {
            msgCounter = message.msgCounter
            timestamp = message.timestamp
            operatorIdType = raw.operatorIdType
            setOperatorId(raw.operatorId)
        }

        ac.operatorid.value = data
    }


    private fun handleMessagePack(
        ac: AircraftObject, message: Message<MessagePack>, timestamp: Long,
        logMessageEntry: LogMessageEntry, msgCounter: Int
    ) {
        val raw = message.payload ?: return

        if (raw.messageSize != Constants.MAX_MESSAGE_SIZE ||
            raw.messagesInPack <= 0 || raw.messagesInPack > Constants.MAX_MESSAGES_IN_PACK
        ) return

        for (i in 0 until raw.messagesInPack) {
            val offset = i * raw.messageSize
            val data = Arrays.copyOfRange(raw.messages, offset, offset + raw.messageSize)
            val subMessage = parseMessage(data, 0, timestamp, logMessageEntry, lastKnownDeviceLocation, msgCounter)
            if (subMessage != null) handleMessages(ac, subMessage)
        }
    }

    companion object {
        private const val TAG = "DronePilotApp"
    }
}