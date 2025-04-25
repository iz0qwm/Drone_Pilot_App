package com.kwos.dronepilotapp.data

class SelfIdData : MessageData() {

    var descriptionType: DescriptionTypeEnum = DescriptionTypeEnum.Text
        private set

    var operationDescription: ByteArray = byteArrayOf()
        private set

    enum class DescriptionTypeEnum {
        Text,
        Emergency,
        Extended_Status {
            override fun toString() = "Ext_Status"
        },
        Invalid;

        companion object {
            fun fromInt(value: Int): DescriptionTypeEnum = when (value) {
                0 -> Text
                1 -> Emergency
                2 -> Extended_Status
                else -> Invalid
            }
        }
    }

    fun setDescriptionType(type: Int) {
        descriptionType = DescriptionTypeEnum.fromInt(type)
    }

    fun setOperationDescription(data: ByteArray) {
        if (data.size <= Constants.MAX_STRING_BYTE_SIZE) {
            operationDescription = data
        }
    }

    val operationDescriptionAsString: String
        get() {
            if (operationDescription.any { (it <= 31 || it >= 127) && it != 0.toByte() }) {
                return "Invalid String"
            }
            return String(operationDescription)
        }
}
