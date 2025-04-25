package com.kwos.dronepilotapp.data

class OperatorIdData : MessageData() {

    var operatorIdType: Int = 0
        set(value) {
            field = value.coerceIn(0, 255)
        }

    var operatorId: ByteArray = byteArrayOf()
        private set

    fun setOperatorId(data: ByteArray) {
        if (data.size <= Constants.MAX_ID_BYTE_SIZE) {
            operatorId = data
        }
    }

    val operatorIdAsString: String
        get() {
            for (c in operatorId) {
                if ((c.toInt() <= 31 || c.toInt() >= 127) && c != 0.toByte()) {
                    return "Invalid String"
                }
            }
            return String(operatorId)
        }
}
