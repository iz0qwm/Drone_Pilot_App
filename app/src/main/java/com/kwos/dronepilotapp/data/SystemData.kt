package com.kwos.dronepilotapp.data


import com.kwos.dronepilotapp.R
import android.content.res.Resources
import java.sql.Timestamp
import java.util.Locale

class SystemData : MessageData() {
    var operatorLocationType: operatorLocationTypeEnum?
        private set
    private var classificationType: classificationTypeEnum?
    private var operatorLatitude = 0.0
    private var operatorLongitude = 0.0
    var areaCount: Int = 0
    var areaRadius: Int = 0
    var areaCeiling: Double
    var areaFloor: Double
    var category: categoryEnum?
        private set
    var classValue: classValueEnum?
        private set
    var operatorAltitudeGeo: Double
    var systemTimestamp: Long = 0

    init {
        operatorLocationType = operatorLocationTypeEnum.Invalid
        classificationType = classificationTypeEnum.Undeclared
        areaCeiling = -1000.0 // -1000 is the Invalid value in the specification
        areaFloor = -1000.0 // -1000 is the Invalid value in the specification
        category = categoryEnum.Undeclared
        classValue = classValueEnum.Undeclared
        operatorAltitudeGeo = -1000.0 // -1000 is the Invalid value in the specification
    }

    // These apply both to operator Latitude/Longitude and to AltitudeGeo
    enum class operatorLocationTypeEnum {
        TakeOff,
        Dynamic,  // Live GNSS Location
        Fixed,  // Fixed Location
        Invalid,
    }

    fun setOperatorLocationType(operatorLocationType: Int) {
        when (operatorLocationType) {
            0 -> this.operatorLocationType = operatorLocationTypeEnum.TakeOff
            1 -> this.operatorLocationType = operatorLocationTypeEnum.Dynamic
            2 -> this.operatorLocationType = operatorLocationTypeEnum.Fixed
            else -> this.operatorLocationType = operatorLocationTypeEnum.Invalid
        }
    }

    enum class classificationTypeEnum {
        Undeclared,
        EU,  // European Union
    }

    fun getclassificationType(): classificationTypeEnum? {
        return classificationType
    }

    fun setClassificationType(classificationType: Int) {
        if (classificationType == 1) {
            this.classificationType = classificationTypeEnum.EU
        } else {
            this.classificationType = classificationTypeEnum.Undeclared
        }
    }

    fun setOperatorLatitude(operatorLatitude: Double) {
        var operatorLatitude = operatorLatitude
        if (operatorLatitude < -90 || operatorLatitude > 90) {
            operatorLatitude = 0.0
            this.operatorLongitude =
                0.0 // both equal to zero is defined in the specification as the Invalid value
        }
        this.operatorLatitude = operatorLatitude
    }

    fun getOperatorLatitude(): Double {
        return operatorLatitude
    }

    fun getOperatorLatitudeAsString(res: Resources): String? {
        if (operatorLatitude == 0.0 && operatorLongitude == 0.0) return res.getString(R.string.unknown)
        return String.format(Locale.US, "%3.7f", operatorLatitude)
    }

    fun setOperatorLongitude(operatorLongitude: Double) {
        var operatorLongitude = operatorLongitude
        if (operatorLongitude < -180 || operatorLongitude > 180) {
            this.operatorLatitude = 0.0
            operatorLongitude =
                0.0 // both equal to zero is defined in the specification as the Invalid value
        }
        this.operatorLongitude = operatorLongitude
    }

    fun getOperatorLongitude(): Double {
        return operatorLongitude
    }

    fun getOperatorLongitudeAsString(res: Resources): String? {
        if (operatorLatitude == 0.0 && operatorLongitude == 0.0) return res.getString(R.string.unknown)
        return String.format(Locale.US, "%3.7f", operatorLongitude)
    }

    val areaRadiusAsString: String
        get() = String.format(Locale.US, "%d m", areaRadius)

    private fun getAltitudeAsString(altitude: Double, res: Resources): String? {
        if (altitude == -1000.0) return res.getString(R.string.unknown)
        return String.format(Locale.US, "%3.1f m", altitude)
    }

    fun getAreaCeilingAsString(res: Resources): String? {
        return getAltitudeAsString(areaCeiling, res)
    }

    fun getAreaFloorAsString(res: Resources): String? {
        return getAltitudeAsString(areaFloor, res)
    }

    enum class categoryEnum {
        Undeclared,
        EU_Open,
        EU_Specific,
        EU_Certified,
    }

    fun setCategory(category: Int) {
        if (classificationType == classificationTypeEnum.EU) {
            when (category) {
                1 -> this.category = categoryEnum.EU_Open
                2 -> this.category = categoryEnum.EU_Specific
                3 -> this.category = categoryEnum.EU_Certified
                else -> this.category = categoryEnum.Undeclared
            }
        } else {
            this.category = categoryEnum.Undeclared
        }
    }

    enum class classValueEnum {
        Undeclared,
        EU_Class_0,
        EU_Class_1,
        EU_Class_2,
        EU_Class_3,
        EU_Class_4,
        EU_Class_5,
        EU_Class_6,
    }

    fun setClassValue(classValue: Int) {
        if (classificationType == classificationTypeEnum.EU) {
            when (classValue) {
                1 -> this.classValue = classValueEnum.EU_Class_0
                2 -> this.classValue = classValueEnum.EU_Class_1
                3 -> this.classValue = classValueEnum.EU_Class_2
                4 -> this.classValue = classValueEnum.EU_Class_3
                5 -> this.classValue = classValueEnum.EU_Class_4
                6 -> this.classValue = classValueEnum.EU_Class_5
                7 -> this.classValue = classValueEnum.EU_Class_6
                else -> this.classValue = classValueEnum.Undeclared
            }
        } else {
            this.classValue = classValueEnum.Undeclared
        }
    }

    fun getOperatorAltitudeGeoAsString(res: Resources): String? {
        return getAltitudeAsString(operatorAltitudeGeo, res)
    }

    fun getSystemTimestampAsString(res: Resources): String? {
        if (systemTimestamp == 0L) return res.getString(R.string.unknown)
        val time = Timestamp((1546300800L + systemTimestamp) * 1000)
        return time.toString()
    }
}