package com.kwos.dronepilotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kwos.dronepilotapp.models.Drone

class DroneAdapter(
    private val droneList: List<Drone>,
    private val onDroneClick: (Drone) -> Unit
) : RecyclerView.Adapter<DroneAdapter.DroneViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DroneViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drone, parent, false)
        return DroneViewHolder(view)
    }

    override fun onBindViewHolder(holder: DroneViewHolder, position: Int) {
        val drone = droneList[position]
        holder.bind(drone)
    }

    override fun getItemCount(): Int = droneList.size

    inner class DroneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageDrone: ImageView = itemView.findViewById(R.id.imageDrone)
        private val textDroneName: TextView = itemView.findViewById(R.id.textDroneName)
        private val textDroneDescription: TextView = itemView.findViewById(R.id.textDroneDescription)

        fun bind(drone: Drone) {
            textDroneName.text = drone.name
            textDroneDescription.text = drone.description
            Glide.with(itemView.context)
                .load(drone.photoUrl)
                .placeholder(R.drawable.ic_drone_placeholder)
                .into(imageDrone)

            itemView.setOnClickListener {
                onDroneClick(drone)
            }
        }
    }
}
