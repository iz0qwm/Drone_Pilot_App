package com.kwos.dronepilotapp

import com.google.android.gms.maps.model.LatLng

data class NoFlyZone(
    val name: String,
    val center: LatLng,
    val radius: Double,
    val lowerLimit: Int,
    val upperLimit: Int,
    val color: Int
)

