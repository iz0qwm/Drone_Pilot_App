package com.kwos.dronepilotapp.adapters

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kwos.dronepilotapp.R
import com.kwos.dronepilotapp.SchedaPilotaActivity

// Modello base per pilota online
data class PilotaOnline(
    val uid: String,
    val name: String,
    val droneName: String,
    val avatarUrl: String? = null
)

class PilotCardAdapter(
    private val context: Context,
    private val lista: List<PilotaOnline>
) : RecyclerView.Adapter<PilotCardAdapter.PilotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PilotViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_pilot_card, parent, false)
        return PilotViewHolder(view)
    }

    override fun onBindViewHolder(holder: PilotViewHolder, position: Int) {
        val pilota = lista[position]
        holder.textPilotName.text = pilota.name
        holder.textDroneInVolo.text = "Con ${pilota.droneName}"

        if (!pilota.avatarUrl.isNullOrEmpty()) {
            Glide.with(context).load(pilota.avatarUrl).into(holder.imageAvatar)
        } else {
            holder.imageAvatar.setImageResource(R.drawable.ic_person_placeholder)
        }

        holder.btnApriScheda.setOnClickListener {
            val intent = Intent(context, SchedaPilotaActivity::class.java)
            intent.putExtra("uid", pilota.uid)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = lista.size

    inner class PilotViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageAvatar: ImageView = view.findViewById(R.id.imagePilotAvatar)
        val textPilotName: TextView = view.findViewById(R.id.textPilotName)
        val textDroneInVolo: TextView = view.findViewById(R.id.textDroneInVolo)
        val btnApriScheda: Button = view.findViewById(R.id.btnApriScheda)
    }
}
