package com.kwos.dronepilotapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
//import com.google.maps.android.clustering.view.ClusterManager

class TakeoffSpotRenderer(
    private val context: Context,
    map: GoogleMap,
    clusterManager: ClusterManager<TakeoffSpotItem>
) : DefaultClusterRenderer<TakeoffSpotItem>(context, map, clusterManager) {

    override fun onBeforeClusterItemRendered(item: TakeoffSpotItem, markerOptions: com.google.android.gms.maps.model.MarkerOptions) {
        // Singolo spot
        markerOptions.icon(item.getIcon())
        markerOptions.title(item.title)
        markerOptions.snippet(item.snippet)
    }

    override fun onBeforeClusterRendered(cluster: Cluster<TakeoffSpotItem>, markerOptions: com.google.android.gms.maps.model.MarkerOptions) {
        // Gruppo di spot (Cluster)
        markerOptions.icon(createClusterIcon(cluster.size))
    }

    private fun createClusterIcon(clusterSize: Int): BitmapDescriptor {
        val diameter = 120
        val bitmap = Bitmap.createBitmap(diameter, diameter, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Cerchio
        val circlePaint = Paint().apply {
            color = Color.parseColor("#3F51B5") // Blu cluster
            isAntiAlias = true
        }
        canvas.drawCircle((diameter / 2).toFloat(), (diameter / 2).toFloat(), (diameter / 2).toFloat(), circlePaint)

        // Numero sopra
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 40f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val xPos = (diameter / 2).toFloat()
        val yPos = (diameter / 2 - (textPaint.descent() + textPaint.ascent()) / 2)

        canvas.drawText(clusterSize.toString(), xPos, yPos, textPaint)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}
