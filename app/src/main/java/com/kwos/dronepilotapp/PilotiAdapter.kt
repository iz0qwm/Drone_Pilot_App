package com.kwos.dronepilotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PilotiAdapter(private val pilotiList: List<String>) : RecyclerView.Adapter<PilotiAdapter.PilotiViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PilotiViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return PilotiViewHolder(view)
    }

    override fun onBindViewHolder(holder: PilotiViewHolder, position: Int) {
        holder.bind(pilotiList[position])
    }

    override fun getItemCount(): Int {
        return pilotiList.size
    }

    class PilotiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nomePilotaTextView: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(nomePilota: String) {
            nomePilotaTextView.text = nomePilota
        }
    }
}
