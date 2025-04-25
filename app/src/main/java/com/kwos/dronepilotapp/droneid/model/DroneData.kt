package com.kwos.dronepilotapp.droneid.model

data class DroneData(
    var macAddress: String = "",
    var operatorId: String? = null,
    var droneId: String = "",
    var idType: Int = 0,
    var idTypeUncategorized: Int = 0,
    var idTypeUaType: Int = 0,
    var idTypeUaClass: Int = 0,
    var idTypeUasId: Int = 0,
    var idTypeSerialNumber: Int = 0,
    var idTypeCaaRegistrationId: Int = 0,
    var idTypeSpecificSessionId: Int = 0,
    var idTypeNone: Int = 0,
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var altitude: Double = 0.0,
    var altitudeType: Int = 0,
    var speed: Double = 0.0,
    var direction: Double = 0.0,
    var verticalSpeed: Double = 0.0,
    var accuracy: Float = 0f,
    var updateTime: Long = 0L,
    var isValid: Boolean = false
) {
    val id: String
        get() = if (droneId.isNotEmpty()) droneId else macAddress

    val lat: Double
        get() = latitude

    val lon: Double
        get() = longitude
}
