package com.kwos.dronepilotapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kwos.dronepilotapp.R
import com.kwos.dronepilotapp.models.Drone

class DroneSchedaAdapter(private val droneList: List<Drone>) : RecyclerView.Adapter<DroneSchedaAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_drone_scheda, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val drone = droneList[position]
        holder.textDroneName.text = drone.name
        holder.textDroneDescription.text = drone.description

        if (drone.photoUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context).load(drone.photoUrl).into(holder.imageDrone)
        } else {
            holder.imageDrone.setImageResource(R.drawable.ic_drone_placeholder)
        }
    }

    override fun getItemCount(): Int = droneList.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageDrone: ImageView = view.findViewById(R.id.imageDrone)
        val textDroneName: TextView = view.findViewById(R.id.textDroneName)
        val textDroneDescription: TextView = view.findViewById(R.id.textDroneDescription)
    }
}
