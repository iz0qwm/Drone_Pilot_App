package com.kwos.dronepilotapp.data

import android.content.res.Resources
import com.kwos.dronepilotapp.R
import java.util.Locale

class LocationData : MessageData() {

    enum class StatusEnum {
        Undeclared, Ground, Airborne, Emergency, Remote_ID_System_Failure;

        override fun toString(): String {
            return if (this == Remote_ID_System_Failure) "Rem_ID_Sys_Fail" else super.toString()
        }

        companion object {
            fun fromInt(value: Int): StatusEnum = values().getOrElse(value) { Undeclared }
        }
    }

    enum class HeightTypeEnum { Takeoff, Ground }
    enum class HorizontalAccuracyEnum {
        Unknown, kilometers_18_52, kilometers_7_408, kilometers_3_704, kilometers_1_852,
        meters_926, meters_555_6, meters_185_2, meters_92_6, meters_30, meters_10, meters_3, meters_1
    }
    enum class VerticalAccuracyEnum {
        Unknown, meters_150, meters_45, meters_25, meters_10, meters_3, meters_1
    }
    enum class SpeedAccuracyEnum {
        Unknown, meter_per_second_10, meter_per_second_3, meter_per_second_1, meter_per_second_0_3
    }

    var status: StatusEnum = StatusEnum.Undeclared
    fun setStatus(value: Int) { status = StatusEnum.fromInt(value) }

    var heightType: HeightTypeEnum = HeightTypeEnum.Takeoff
    fun setHeightType(value: Int) {
        heightType = if (value == 1) HeightTypeEnum.Ground else HeightTypeEnum.Takeoff
    }

    var direction: Double = 361.0
        set(value) { field = if (value < 0 || value > 360) 361.0 else value }

    var speedHorizontal: Double = 255.0
        set(value) { field = if (value < 0 || value > 254.25) 255.0 else value }

    var speedVertical: Double = 63.0
        set(value) { field = if (value < -62 || value > 62) 63.0 else value }

    var latitude: Double = 0.0
        set(value) {
            if (value < -90 || value > 90) {
                field = 0.0
                longitude = 0.0
            } else field = value
        }

    var longitude: Double = 0.0
        set(value) {
            if (value < -180 || value > 180) {
                latitude = 0.0
                field = 0.0
            } else field = value
        }

    var altitudePressure: Double = -1000.0
        set(value) { field = if (value < -1000 || value > 31767) -1000.0 else value }

    var altitudeGeodetic: Double = -1000.0
        set(value) { field = if (value < -1000 || value > 31767) -1000.0 else value }

    var height: Double = -1000.0
        set(value) { field = if (value < -1000 || value > 31767) -1000.0 else value }

    var horizontalAccuracy: HorizontalAccuracyEnum = HorizontalAccuracyEnum.Unknown
    fun setHorizontalAccuracy(value: Int) {
        horizontalAccuracy = HorizontalAccuracyEnum.values().getOrElse(value) { HorizontalAccuracyEnum.Unknown }
    }

    var verticalAccuracy: VerticalAccuracyEnum = VerticalAccuracyEnum.Unknown
    fun setVerticalAccuracy(value: Int) { verticalAccuracy = intToVerticalAccuracy(value) }

    var baroAccuracy: VerticalAccuracyEnum = VerticalAccuracyEnum.Unknown
    fun setBaroAccuracy(value: Int) { baroAccuracy = intToVerticalAccuracy(value) }

    private fun intToVerticalAccuracy(value: Int): VerticalAccuracyEnum =
        VerticalAccuracyEnum.values().getOrElse(value) { VerticalAccuracyEnum.Unknown }

    var speedAccuracy: SpeedAccuracyEnum = SpeedAccuracyEnum.Unknown
    fun setSpeedAccuracy(value: Int) {
        speedAccuracy = SpeedAccuracyEnum.values().getOrElse(value) { SpeedAccuracyEnum.Unknown }
    }

    var locationTimestamp: Double = 0xFFFF.toDouble()
        set(value) {
            field = when {
                value < 0 -> 0.0
                value != 0xFFFF.toDouble() && value > 36000 -> 36000.0
                else -> value
            }
        }

    var timeAccuracy: Double = 0.0
        set(value) {
            field = when {
                value < 0 -> 0.0
                value > 1.5 -> 1.5
                else -> value
            }
        }

    var distance: Float = 0f
        set(value) { field = value }

    // ---------------- DISPLAY HELPERS ----------------

    fun getDirectionAsString(res: Resources): String =
        if (direction != 361.0) String.format(Locale.US, "%3.0f deg", direction) else res.getString(R.string.unknown)

    fun getSpeedHorizontalAsString(res: Resources): String =
        if (speedHorizontal != 255.0) String.format(Locale.US, "%3.2f m/s", speedHorizontal) else res.getString(R.string.unknown)

    fun getSpeedHorizontalLessPreciseAsString(res: Resources): String =
        if (speedHorizontal != 255.0) String.format(Locale.US, "%3.0fm/s", speedHorizontal) else res.getString(R.string.unknown)

    fun getSpeedVerticalAsString(res: Resources): String =
        if (speedVertical != 63.0) String.format(Locale.US, "%3.2f m/s", speedVertical) else res.getString(R.string.unknown)

    fun getLatitudeAsString(res: Resources): String =
        if (latitude == 0.0 && longitude == 0.0) res.getString(R.string.unknown)
        else String.format(Locale.US, "%3.7f", latitude)

    fun getLongitudeAsString(res: Resources): String =
        if (latitude == 0.0 && longitude == 0.0) res.getString(R.string.unknown)
        else String.format(Locale.US, "%3.7f", longitude)

    private fun getAltitudeAsString(value: Double, res: Resources): String =
        if (value == -1000.0) res.getString(R.string.unknown)
        else String.format(Locale.US, "%3.1f m", value)

    fun getAltitudePressureAsString(res: Resources): String = getAltitudeAsString(altitudePressure, res)
    fun getAltitudeGeodeticAsString(res: Resources): String = getAltitudeAsString(altitudeGeodetic, res)
    fun getHeightAsString(res: Resources): String = getAltitudeAsString(height, res)

    fun getHeightLessPreciseAsString(res: Resources): String =
        if (height == -1000.0) res.getString(R.string.unknown)
        else String.format(Locale.US, "%3.0fm", height)

    fun getHorizontalAccuracyAsString(res: Resources): String =
        when (horizontalAccuracy) {
            HorizontalAccuracyEnum.kilometers_18_52 -> "< 18.52 km"
            HorizontalAccuracyEnum.kilometers_7_408 -> "< 7.408 km"
            HorizontalAccuracyEnum.kilometers_3_704 -> "< 3.704 km"
            HorizontalAccuracyEnum.kilometers_1_852 -> "< 1.852 km"
            HorizontalAccuracyEnum.meters_926 -> "< 926 m"
            HorizontalAccuracyEnum.meters_555_6 -> "< 555.6 m"
            HorizontalAccuracyEnum.meters_185_2 -> "< 185.2 m"
            HorizontalAccuracyEnum.meters_92_6 -> "< 92.6 m"
            HorizontalAccuracyEnum.meters_30 -> "< 30 m"
            HorizontalAccuracyEnum.meters_10 -> "< 10 m"
            HorizontalAccuracyEnum.meters_3 -> "< 3 m"
            HorizontalAccuracyEnum.meters_1 -> "< 1 m"
            else -> res.getString(R.string.unknown)
        }

    fun getVerticalAccuracyAsString(value: VerticalAccuracyEnum, res: Resources): String =
        when (value) {
            VerticalAccuracyEnum.meters_150 -> "< 150 m"
            VerticalAccuracyEnum.meters_45 -> "< 45 m"
            VerticalAccuracyEnum.meters_25 -> "< 25 m"
            VerticalAccuracyEnum.meters_10 -> "< 10 m"
            VerticalAccuracyEnum.meters_3 -> "< 3 m"
            VerticalAccuracyEnum.meters_1 -> "< 1 m"
            else -> res.getString(R.string.unknown)
        }

    fun getSpeedAccuracyAsString(res: Resources): String =
        when (speedAccuracy) {
            SpeedAccuracyEnum.meter_per_second_10 -> "< 10 m/s"
            SpeedAccuracyEnum.meter_per_second_3 -> "< 3 m/s"
            SpeedAccuracyEnum.meter_per_second_1 -> "< 1 m/s"
            SpeedAccuracyEnum.meter_per_second_0_3 -> "< 0.3 m/s"
            else -> res.getString(R.string.unknown)
        }

    fun getLocationTimestampAsString(): String =
        if (locationTimestamp == 0xFFFF.toDouble()) "--:--"
        else String.format(Locale.US, "%02.0f:%02.0f", locationTimestamp.toInt() / 600, (locationTimestamp / 10) % 60)

    fun getTimeAccuracyAsString(res: Resources): String =
        if (timeAccuracy == 0.0) res.getString(R.string.unknown)
        else String.format(Locale.US, "<= %1.1f s", timeAccuracy)

    fun getDistanceAsString(): String = String.format(Locale.US, "~%.0f m", distance)
}