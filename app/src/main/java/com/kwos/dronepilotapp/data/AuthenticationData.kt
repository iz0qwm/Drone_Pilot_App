package com.kwos.dronepilotapp.data

import android.content.res.Resources
import com.kwos.dronepilotapp.R
import java.sql.Timestamp
import java.util.Locale

class AuthenticationData : MessageData() {

    enum class AuthTypeEnum(val id: Int) {
        None(0),
        UAS_ID_Signature(1),
        Operator_ID_Signature(2),
        Message_Set_Signature(3),
        Network_Remote_ID(4),
        Specific_Authentication(5),
        Private_Use_0xA(0xA),
        Private_Use_0xB(0xB),
        Private_Use_0xC(0xC),
        Private_Use_0xD(0xD),
        Private_Use_0xE(0xE),
        Private_Use_0xF(0xF);

        companion object {
            fun fromId(id: Int): AuthTypeEnum {
                return values().find { it.id == id } ?: None
            }
        }
    }

    var authType: AuthTypeEnum = AuthTypeEnum.None
    var authDataPage: Int = 0
        set(value) {
            field = value.coerceIn(0, Constants.MAX_AUTH_DATA_PAGES - 1)
        }

    var authLastPageIndex: Int = 0
        set(value) {
            field = value.coerceIn(0, Constants.MAX_AUTH_DATA_PAGES - 1)
        }

    var authLength: Int = 0
        set(value) {
            field = value.coerceIn(0, Constants.MAX_AUTH_DATA)
        }

    var authTimestamp: Long = 0
    var authData: ByteArray = byteArrayOf()

    fun setAuthType(typeId: Int) {
        authType = AuthTypeEnum.fromId(typeId)
    }

    val authLastPageIndexAsString: String
        get() = String.format(Locale.US, "%d pages", authLastPageIndex)

    val authLengthAsString: String
        get() = String.format(Locale.US, "%d bytes", authLength)

    fun getAuthTimestampAsString(res: Resources): String {
        return if (authTimestamp == 0L) {
            res.getString(R.string.unknown)
        } else {
            val time = Timestamp((1546300800L + authTimestamp) * 1000)
            time.toString()
        }
    }

    val authenticationDataAsString: String
        get() = buildString {
            for (i in 0 until authLength.coerceAtMost(authData.size)) {
                append(String.format("%02X ", authData[i]))
            }
        }
}
