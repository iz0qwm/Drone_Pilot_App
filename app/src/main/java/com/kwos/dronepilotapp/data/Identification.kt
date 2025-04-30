package com.kwos.dronepilotapp.data

class Identification : MessageData() {

    enum class UaTypeEnum {
        None,
        Aeroplane,
        Helicopter_or_Multirotor,
        Gyroplane,
        Hybrid_Lift,
        Ornithopter,
        Glider,
        Kite,
        Free_balloon,
        Captive_balloon,
        Airship,
        Free_fall_parachute,
        Rocket,
        Tethered_powered_aircraft,
        Ground_obstacle,
        Other;

        companion object {
            fun fromInt(value: Int): UaTypeEnum {
                return values().getOrElse(value) { None }
            }
        }
    }

    enum class IdTypeEnum {
        None,
        Serial_Number,
        CAA_Registration_ID,
        UTM_Assigned_ID,
        Specific_Session_ID;

        companion object {
            fun fromInt(value: Int): IdTypeEnum {
                return values().getOrElse(value) { None }
            }
        }
    }

    var uaType: UaTypeEnum = UaTypeEnum.None
        private set

    var idType: IdTypeEnum = IdTypeEnum.None
        private set

    var uasId: ByteArray = byteArrayOf()
        private set

    fun setUaType(type: Int) {
        uaType = UaTypeEnum.fromInt(type)
    }

    fun setIdType(type: Int) {
        idType = IdTypeEnum.fromInt(type)
    }

    fun setUasId(data: ByteArray) {
        if (data.size <= Constants.MAX_ID_BYTE_SIZE) {
            uasId = data
        }
    }

    val uasIdAsString: String
        get() {
            return when (idType) {
                IdTypeEnum.Serial_Number, IdTypeEnum.CAA_Registration_ID -> {
                    // Filtra solo byte ASCII stampabili
                    val asciiBytes = uasId.takeWhile { it in 32..126 }
                    if (asciiBytes.isNotEmpty()) {
                        String(asciiBytes.toByteArray()).trim()
                    } else {
                        // fallback esadecimale se non è leggibile
                        "0x" + uasId.joinToString("") { String.format("%02X", it) }
                    }
                }
                IdTypeEnum.UTM_Assigned_ID, IdTypeEnum.Specific_Session_ID -> {
                    "0x" + uasId.joinToString("") { String.format("%02X", it) }
                }
                else -> ""
            }
        }



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Identification) return false

        return uaType == other.uaType &&
                idType == other.idType &&
                uasId.contentEquals(other.uasId)
    }

    override fun hashCode(): Int {
        var result = uaType.hashCode()
        result = 31 * result + idType.hashCode()
        result = 31 * result + uasId.contentHashCode()
        return result
    }
}
