// Documento.kt
package com.kwos.dronepilotapp.models

data class Documento(
    val id: String = "",
    val title: String = "",
    val type: String = "",
    val expiryDate: String = "",
    val fileUrl: String = "",
    val renewalUrl: String = "",
    val policyNumber: String = "",
    val tesseraNumber: String = ""
)