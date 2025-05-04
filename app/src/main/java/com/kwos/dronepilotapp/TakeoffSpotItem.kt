package com.kwos.dronepilotapp

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.maps.android.clustering.ClusterItem

class TakeoffSpotItem(
    private val lat: Double,
    private val lng: Double,
    private val title: String,
    private val snippet: String,
    private val iconBitmap: BitmapDescriptor,
    val photoUrl: String // aggiunta per dettagli
) : ClusterItem {

    override fun getPosition(): LatLng {
        return LatLng(lat, lng)
    }

    override fun getTitle(): String {
        return title
    }

    override fun getSnippet(): String {
        return snippet
    }

    fun getIcon(): BitmapDescriptor {
        return iconBitmap
    }
}

