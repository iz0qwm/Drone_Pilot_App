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
                    if (uasId.any { it.toInt() <= 31 || it.toInt() >= 127 && it != 0.toByte() }) {
                        "Invalid ID String"
                    } else {
                        String(uasId)
                    }
                }
                IdTypeEnum.UTM_Assigned_ID, IdTypeEnum.Specific_Session_ID -> {
                    buildString {
                        append("0x")
                        uasId.forEach { append(String.format("%02X", it)) }
                    }
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
