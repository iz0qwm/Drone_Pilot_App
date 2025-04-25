package com.kwos.dronepilotapp.data

data class LogEntry(
    var session: Int = 0,
    var timestamp: Long = 0L,
    var transportType: String = "",
    var macAddress: String = "",
    var msgVersion: Int = 0,
    var rssi: Int = 0,
    var data: ByteArray = byteArrayOf(),
    var csvLog: StringBuilder = StringBuilder()
) {

    override fun toString(): String {
        return listOf(
            session.toString(),
            timestamp.toString(),
            transportType,
            macAddress,
            msgVersion.toString(),
            rssi.toString(),
            toHexString(data, data.size),
            csvLog.toString()
        ).joinToString(DELIM)
    }

    companion object {
        val HEADER = listOf(
            "session",
            "timestamp (nanos)",
            "transportType",
            "macAddress",
            "msgVersion",
            "rssi",
            "payload"
        )

        const val DELIM = ","

        fun fromString(line: String): LogEntry? {
            val fields = line.split("\\s*[,]\\s*".toRegex())
            if (fields.size < 7) return null

            return try {
                LogEntry(
                    session = fields[0].toInt(),
                    timestamp = fields[1].toLong(),
                    transportType = fields[2],
                    macAddress = fields[3],
                    msgVersion = fields[4].toInt(),
                    rssi = fields[5].toInt(),
                    data = parseHexString(fields[6])
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun toHexString(bytes: ByteArray, len: Int): String {
            return bytes.take(len).joinToString(" ") {
                String.format("%02X", it)
            }
        }

        private fun parseHexString(hexString: String): ByteArray {
            val byteStrings = hexString.trim().split("\\s+".toRegex())
            return byteStrings.map { it.toInt(16).toByte() }.toByteArray()
        }
    }
}
