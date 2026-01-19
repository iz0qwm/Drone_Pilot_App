package com.kwos.dronepilotapp.droneid

import android.bluetooth.le.ScanResult
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
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
import com.kwos.dronepilotapp.droneid.PlausibilityFilter
import com.kwos.dronepilotapp.droneid.DroneSample

class OpenDroneIdDataManager(private val callback: Callback) {

    init {
        initDroneMaps()
    }

    // --- Forward verso Drone Sky Check ---
    private val lastForwardTs = ConcurrentHashMap<Long, Long>()
    private val FORWARD_INTERVAL_MS = 3_000L

    val aircraft = ConcurrentHashMap<Long, AircraftObject>()
    var lastKnownDeviceLocation: android.location.Location? = null
    private lateinit var prefixMap: Map<String, String>
    private lateinit var modelMap: Map<String, String>
    private val plausibility = PlausibilityFilter(
        receiverLat = { lastKnownDeviceLocation?.latitude },
        receiverLon = { lastKnownDeviceLocation?.longitude },
        maxRangeMeters = 1000.0,
        minAltM = -120.0,
        maxAltM = 1200.0,
        maxSpeedMS = 50.0,
        jumpSpeedCapMS = 60.0,
        confirmWindowMs = 10_000L,
        staleMs = 30_000L
    ).apply {
        onDrop = { s, reason ->
            Log.d(TAG, "Plausibility DROP [$reason] mac=${s.mac} lat=${s.lat} lon=${s.lon} alt=${s.altitudeM} v=${s.speedMS}")
        }
    }

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

        //Log.d("DronePilotApp", "🔄 Chiamo receiveData da Bluetooth per MAC=$macAddress")

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
        timeNano: Long,
        macAddress: String,
        macAddressLong: Long,
        rssi: Int,
        message: Message<*>,
        logMessageEntry: LogMessageEntry,
        transportType: String?
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

        val payload = message.payload
        //Log.d("DronePilotApp", "OpenDroneIdManager: 🔍 Tipo payload = ${payload?.javaClass?.simpleName}")

        // ✅ SALVATAGGIO ID LEGGIBILE (solo se valido)
        try {
            if (payload is BasicId) {
                val uasIdBytes = payload.uasId
                val hexString = uasIdBytes.joinToString("") { "%02X".format(it) }

                val uasIdString = uasIdBytes
                    .takeWhile { it in 32..126 } // tronca ai caratteri validi
                    .map { it.toInt().toChar() }
                    .joinToString("")
                    .trim()

                //Log.d("DronePilotApp", "🧪 BasicId ricevuto (HEX): $hexString")
                //Log.d("DronePilotApp", "🧪 BasicId interpretato: $uasIdString")

                if (uasIdString.matches(Regex("^[A-Za-z0-9\\-_:]{5,}.*")) &&
                    ac.uasIdString.value.isNullOrBlank()
                ) {
                    ac.uasIdString.value = uasIdString
                    Log.d("DronePilotApp", "✅ ID leggibile salvato: $uasIdString")

                    if (ac.location.value != null) {
                        callback.onLocationUpdate(ac)
                    }
                } else {
                    //Log.d("DronePilotApp", "❌ ID NON salvato: non leggibile o già presente (HEX: $hexString)")
                }
            }
        } catch (e: Exception) {
            Log.w("DronePilotApp", "⚠️ Errore durante parsing BasicId: ${e.message}")
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

        // ✅ Verifica se possiamo notificare aggiornamento completo
        if (ac.location.value != null) {

            val now = System.currentTimeMillis()
            val droneKey = ac.macAddress
            val last = lastForwardTs[droneKey] ?: 0L

            if (now - last > FORWARD_INTERVAL_MS) {
                lastForwardTs[droneKey] = now
                forwardToDSC(ac)
            }
        }



        logMessageEntry.msgVersion = ac.connection.value?.msgVersion ?: 0
    }



    private fun createNewAircraft(macAddress: String, macAddressLong: Long): AircraftObject {
        return AircraftObject(macAddressLong).apply {
            macAddressString = macAddress // 👈 memorizziamo la stringa per uso futuro
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

        // 🛡️ 1) Prepara i valori “crudi” da validare
        val macStr = ac.connection.value?.macAddress ?: ac.macAddressString ?: "UNKNOWN"
        Log.d(TAG, "OpenDroneIdManager: 🚚 LOCATION arrivato (prima del filtro) mac=$macStr lat=${raw.latitude} lon=${raw.longitude}")
        val lat = raw.latitude
        val lon = raw.longitude

        // scegliamo una quota “migliore disponibile”
        val altGeod = raw.getAltitudeGeodetic()
        val altPress = raw.getAltitudePressure()
        val altitudeM = when {
            altGeod != null && !altGeod.isNaN() && altGeod.isFinite() -> altGeod
            altPress != null && !altPress.isNaN() && altPress.isFinite() -> altPress
            else -> null
        }

        // velocità orizzontale in m/s (255 = invalida)
        val spdH = raw.getSpeedHori()
        val speedMS = if (spdH.isFinite() && spdH in 0.0..200.0) spdH else null

        val sample = DroneSample(
            mac = macStr,
            lat = lat,
            lon = lon,
            altitudeM = altitudeM,
            speedMS = speedMS,
            tsMillis = System.currentTimeMillis()
        )

        // 🧹 2) Filtro di plausibilità: se non passa, scarta questo pacchetto
        if (!plausibility.isPlausible(sample)) {
            Log.d(TAG, "OpenDroneIdDataManager: ❌ sample scartato (plausibility) lat=$lat lon=$lon alt=$altitudeM v=$speedMS mac=$macStr")
            return
        }

        Log.d(TAG, "OpenDroneIdManager: ✅ LOCATION passato il filtro mac=$macStr")

        // ✅ 3) Se è plausibile, allora mappiamo nei tuoi oggetti e proseguiamo
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

        Log.d(TAG, "OpenDroneIdManager: 📡 Location OK: lat=${raw.latitude}, lon=${raw.longitude}, alt=${raw.getAltitudeGeodetic()}, speedH=${raw.getSpeedHori()} rssi=${ac.connection.value?.rssi}")
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

    private fun forwardToDSC(ac: AircraftObject) {
        val loc = ac.location.value ?: return

        val data = hashMapOf(
            "droneId" to (ac.uasIdString.value ?: ac.macAddressString.ifEmpty { ac.macAddress.toString() }),
            "lat" to loc.latitude,
            "lon" to loc.longitude,
            "altitude" to loc.height.takeIf { it != -1000.0 },
            "speed" to loc.speedHorizontal,
            "ts" to System.currentTimeMillis(),
            "heading" to loc.direction.takeIf { it in 0.0..360.0 },
            "model" to resolveModelName(ac),
            "source" to "drone_pilot_app",
            "identityType" to if (!ac.uasIdString.value.isNullOrBlank()) "uas_id" else "mac"

        )

        FirebaseFunctions.getInstance()
            .getHttpsCallable("forwardDronePositionToDSC")
            .call(data)
    }

    private fun resolveModelName(ac: AircraftObject): String? {
        if (!::prefixMap.isInitialized || !::modelMap.isInitialized) {
            Log.w(TAG, "Model maps not initialized, skipping model resolution")
            return null
        }

        val uasId = ac.uasIdString.value
            ?: ac.identification1.value?.uasIdAsString
            ?: return null

        val (manufacturer, model) = parseUasId(uasId, prefixMap, modelMap)

        return if (
            model != "Modello sconosciuto" &&
            manufacturer != "Costruttore sconosciuto"
        ) {
            "$model ($manufacturer)"
        } else null
    }


    //
    // Drone ID - Mappe Costruttori - Modelli
    //
    private fun initDroneMaps() {
        prefixMap = mapOf(
            "1581F" to "DJI",
            "1581E" to "DJI",
            "1234A" to "Parrot",
            "5678B" to "Autel",
            "1748F" to "Autel",
            "1748C" to "Autel",
            "1596"  to "Dronetag",
            "2106"  to "TopView Pollicino"
        )

        modelMap = mapOf(
            "1ZP" to "Mavic 2 Pro",
            "163" to "Mavic 2 Pro",
            "1KP" to "Mavic 2 Zoom",
            "0ZP" to "Mavic Air 2",
            "0M6" to "Mavic 2 Zoom",
            "1WN" to "Mavic Air 2",
            "3ZP" to "Phantom 4 Pro V2.0",
            "2ZP" to "Phantom 4 Advanced",
            "3N3" to "Mavic Air 2",
            "5ZP" to "Inspire 2",
            "446" to "Agras T30",
            "4GC" to "Mavic 2E",
            "4ZP" to "Mavic Mini",
            "45T" to "Mavic 3",
            "4QW" to "Avata",
            "4QZ" to "Mavic 3 Cine",
            "4XF" to "Mini 3 Pro",
            "5FJ" to "Mavic 3 Thermal",
            "5YH" to "Mini 3",
            "574" to "Agras T40",
            "6BU" to "Agras T50",
            "6Z9" to "Mini 4 Pro",
            "67P" to "Mavic 3 Classic",
            "67Q" to "Mavic 3 Pro",
            "6N8" to "Air 3",
            "6W8" to "Avata 2",
            "7ZP" to "Air 2S",
            "3YT" to "Air 2S",
            "8ZP" to "Mini 2",
            "895" to "Air 3S",
            "9DE" to "Mini 5 Pro",
            "7FV" to "Matrice 4E",
            "7K3" to "Matrice 4T",
            "8HH" to "Matrice 4D",
            "8HG" to "Matrice 4 TD",
            "986" to "Mavic 4 Pro",

            "JD2" to "Dragonfish Lite",
            "JD3" to "Dragonfish Pro",
            "JD1" to "Dragonfish Std",
            "EV2" to "EVO II V3",
            "EV3" to "EVO Max",
            "EV5" to "EVO Lite",
            "V4A" to "Autel Alpha",

            "A34" to "Beacon"

        )
    }

    private fun parseUasId(uasId: String, prefixMap: Map<String, String>, modelMap: Map<String, String>): Pair<String, String> {
        if (uasId.length < 7) return Pair("Costruttore sconosciuto", "Modello sconosciuto")

        // Cerca il prefisso più lungo corrispondente
        val manufacturerEntry = prefixMap.entries
            .firstOrNull { uasId.startsWith(it.key) }

        val manufacturer = manufacturerEntry?.value ?: "Costruttore sconosciuto"
        val prefixLength = manufacturerEntry?.key?.length ?: 0

        val serialPart = uasId.drop(prefixLength)
        val modelKey = serialPart.take(3).uppercase()

        val model = modelMap[modelKey] ?: "Modello sconosciuto"

        return Pair(manufacturer, model)
    }

    companion object {
        private const val TAG = "DronePilotApp"
    }
}