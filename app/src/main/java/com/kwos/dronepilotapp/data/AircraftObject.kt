package com.kwos.dronepilotapp.data

import androidx.lifecycle.MutableLiveData

class AircraftObject(val macAddress: Long) {

    val connection = MutableLiveData<Connection?>()
    val identification1 = MutableLiveData<Identification?>()
    val identification2 = MutableLiveData<Identification?>()
    val id1Shadow = MutableLiveData<Identification?>()
    val id2Shadow = MutableLiveData<Identification?>()
    val location = MutableLiveData<LocationData?>()
    val authentication = MutableLiveData<AuthenticationData?>()
    val selfid = MutableLiveData<SelfIdData?>()
    val system = MutableLiveData<SystemData?>()
    val operatorid = MutableLiveData<OperatorIdData?>()

    private var authLastPageIndexSave = 0
    private var authLengthSave = 0
    private var authTimestampSave: Long = 0
    private val authDataCombined = ByteArray(Constants.MAX_AUTH_DATA)

    fun getConnection(): Connection? = connection.value
    fun getIdentification1(): Identification? = identification1.value
    fun getIdentification2(): Identification? = identification2.value
    fun getLocation(): LocationData? = location.value
    fun getAuthentication(): AuthenticationData? = authentication.value
    fun getSelfID(): SelfIdData? = selfid.value
    fun getSystem(): SystemData? = system.value
    fun getOperatorID(): OperatorIdData? = operatorid.value

    fun combineAuthentication(newData: AuthenticationData): AuthenticationData {
        val currData = authentication.value ?: AuthenticationData()

        currData.msgCounter = newData.msgCounter
        currData.timestamp = newData.timestamp
        currData.msgVersion = newData.msgVersion

        val offset: Int
        val amount: Int

        if (newData.authDataPage == 0) {
            authLastPageIndexSave = newData.authLastPageIndex
            authLengthSave = newData.authLength
            authTimestampSave = newData.authTimestamp
            offset = 0
            amount = Constants.MAX_AUTH_PAGE_ZERO_SIZE
        } else {
            offset = Constants.MAX_AUTH_PAGE_ZERO_SIZE +
                    (newData.authDataPage - 1) * Constants.MAX_AUTH_PAGE_NON_ZERO_SIZE
            amount = Constants.MAX_AUTH_PAGE_NON_ZERO_SIZE
        }

        for (i in offset until offset + amount) {
            if (i < newData.authData.size && i < authDataCombined.size) {
                authDataCombined[i] = newData.authData[i]
            }
        }

        currData.authType = newData.authType
        currData.authLastPageIndex = authLastPageIndexSave
        currData.authLength = authLengthSave
        currData.authTimestamp = authTimestampSave
        currData.authData = authDataCombined

        return currData
    }

    private var idToShow = 0

    fun updateShadowBasicId() {
        when (idToShow) {
            0 -> {
                id1Shadow.value = identification1.value
                idToShow++
            }
            3 -> {
                val id2 = identification2.value
                if (id2 != null && id2.idType != Identification.IdTypeEnum.None) {
                    id2Shadow.value = identification2.value
                }
                idToShow++
            }
            6 -> idToShow = 0
            else -> idToShow++
        }
    }

    override fun toString(): String {
        return "AircraftObject(macAddress=$macAddress, identification1=$identification1, identification2=$identification2)"
    }
}
