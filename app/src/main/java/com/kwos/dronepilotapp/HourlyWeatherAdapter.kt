package com.kwos.dronepilotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color

class HourlyWeatherAdapter(private val forecastList: List<HourlyForecast>) :
    RecyclerView.Adapter<HourlyWeatherAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeTextView: TextView = view.findViewById(R.id.timeTextView)
        val tempTextView: TextView = view.findViewById(R.id.tempTextView)
        val seaLevelTextView: TextView = view.findViewById(R.id.seaLevelTextView)
        val cloudsTextView: TextView = view.findViewById(R.id.cloudsTextView)
        val windTextView: TextView = view.findViewById(R.id.windTextView)
        val rainTextView: TextView = view.findViewById(R.id.rainTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_forecast, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val forecast = forecastList[position]
        holder.timeTextView.text = forecast.dt
        holder.tempTextView.text = "${forecast.temp}"
        holder.seaLevelTextView.text = "${forecast.pressure}"
        holder.cloudsTextView.text = "${forecast.clouds}"
        holder.windTextView.text = "${forecast.windSpeed}"
        holder.rainTextView.text = "${forecast.windGust}"

        // Colore per la temperatura
        when {
            forecast.temp <= 2 -> holder.tempTextView.setTextColor(Color.BLUE) // Blue per temperature <= 2
            forecast.temp in 30.0..35.0 -> holder.tempTextView.setTextColor(Color.parseColor("#FFA500")) // Arancione per temperature tra 30 e 35
            forecast.temp > 35 -> holder.tempTextView.setTextColor(Color.RED) // Rosso per temperature > 35
            else -> holder.tempTextView.setTextColor(Color.BLACK) // Default
        }

        // Colore per la copertura nuvolosa
        when {
            forecast.clouds in 40..70 -> holder.cloudsTextView.setTextColor(Color.parseColor("#FFA500")) // Arancione per nuvolosità tra 40 e 70
            forecast.clouds > 70 -> holder.cloudsTextView.setTextColor(Color.RED) // Rosso per nuvolosità > 70
            else -> holder.cloudsTextView.setTextColor(Color.BLACK) // Default
        }

        // Colore per la velocità del vento
        when {
            forecast.windSpeed > 20 -> holder.windTextView.setTextColor(Color.RED) // Rosso per vento > 20 km/h
            forecast.windSpeed in 10..20 -> holder.windTextView.setTextColor(Color.parseColor("#FFA500")) // Arancione per vento tra 10 e 20 km/h
            else -> holder.windTextView.setTextColor(Color.BLACK) // Default
        }

        // Colore per la raffica del vento
        when {
            forecast.windGust > 20 -> holder.rainTextView.setTextColor(Color.RED) // Rosso per raffica > 20 km/h
            forecast.windGust in 10..20 -> holder.rainTextView.setTextColor(Color.parseColor("#FFA500")) // Arancione per raffica tra 10 e 20 km/h
            else -> holder.rainTextView.setTextColor(Color.BLACK) // Default
        }
    }

    override fun getItemCount() = forecastList.size
}
