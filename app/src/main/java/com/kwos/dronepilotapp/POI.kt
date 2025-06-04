package com.kwos.dronepilotapp

data class POI(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val type: String = "poi"
)
