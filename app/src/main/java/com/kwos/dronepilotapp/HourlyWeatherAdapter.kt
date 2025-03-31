package com.kwos.dronepilotapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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
        holder.timeTextView.text = forecast.dt_txt
        holder.tempTextView.text = "${forecast.temperature} °C"
        holder.seaLevelTextView.text = "${forecast.seaLevel} hPa"
        holder.cloudsTextView.text = "${forecast.cloudiness} %"
        holder.windTextView.text = "${forecast.windSpeed} m/s"
        holder.rainTextView.text = "${forecast.rainVolume} mm"
    }

    override fun getItemCount() = forecastList.size
}
