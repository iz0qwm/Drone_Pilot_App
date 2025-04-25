package com.kwos.dronepilotapp.data


object Constants {
    const val REQUEST_ENABLE_BT: Int = 1
    const val FINE_LOCATION_PERMISSION_REQUEST_CODE: Int = 2
    const val REQUEST_ENABLE_WIFI: Int = 3
    const val REQUEST_BLUETOOTH_PERMISSION_SCAN: Int = 4
    const val REQUEST_BLUETOOTH_PERMISSION_CONNECT: Int = 5
    const val REQUEST_NEARBY_WIFI_DEVICES_PERMISSION: Int = 6

    const val DELIM: String = ","

    const val MAX_ID_BYTE_SIZE: Int = 20
    const val MAX_STRING_BYTE_SIZE: Int = 23
    const val MAX_AUTH_DATA_PAGES: Int = 16
    const val MAX_AUTH_PAGE_ZERO_SIZE: Int = 17
    const val MAX_AUTH_PAGE_NON_ZERO_SIZE: Int = 23
    val MAX_AUTH_DATA: Int =
        MAX_AUTH_PAGE_ZERO_SIZE + (MAX_AUTH_DATA_PAGES - 1) * MAX_AUTH_PAGE_NON_ZERO_SIZE
    const val MAX_MESSAGE_SIZE: Int = 25
    const val MAX_MESSAGES_IN_PACK: Int = 9
    val MAX_MESSAGE_PACK_SIZE: Int = MAX_MESSAGE_SIZE * MAX_MESSAGES_IN_PACK

    /* The continued development of the relevant standards is reflected in the remote ID protocol
 * version number that is transmitted in the header of each drone ID message.
 *
 * The following protocol versions have been in use:
 * 0. ASTM F3411-19. Published Feb 14, 2020. https://www.astm.org/f3411-19.html
 * 1. ASD-STAN prEN 4709-002 P1. Published 31-Oct-2021.
 *    http://asd-stan.org/downloads/asd-stan-pren-4709-002-p1/
 *
 *    ASTM F3411 v1.1 draft sent for first ballot round autumn 2021
 *
 * 2. ASTM F3411 v1.1 draft sent for second ballot round Q1 2022. (ASTM F3411-22 ?)
 *    The delta to protocol version 1 is small:
 *    - New enum values:
 *      LocationData.StatusEnum.Remote_ID_System_Failure
 *      SelfIdData.descriptionTypeEnum.Emergency,
 *      SelfIdData.descriptionTypeEnum.Extended_Status,
 *    - New Timestamp field in the System message
 *
 * Since the strategy of the standardization for drone ID has been to not break backwards
 * compatibility when adding new functionality, this implementation allows decoding messages
 * with a higher version number than defined below. It is assumed that newer versions can be
 * decoded but some data elements might be missing in the output. The message version displayed
 * in the detailed info view will be drawn with red color in this case. */
    const val MAX_MSG_VERSION: Int = 2
}