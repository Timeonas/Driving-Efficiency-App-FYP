package com.example.drivingefficiencyapp.viewLayer.trip

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.modelLayer.trip.Trip

class TripAdapter(
    private val trips: MutableList<Trip> = mutableListOf(),
    private val onDeleteClicked: (Trip) -> Unit,
    private val onTripClicked: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.tripDate)
        val durationText: TextView = itemView.findViewById(R.id.tripDuration)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        val efficiencyScore: TextView = itemView.findViewById(R.id.efficiencyScore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.trip_item, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val trip = trips[position]

        holder.dateText.text = trip.date

        val durationInfo = StringBuilder()
        durationInfo.append(holder.itemView.context.getString(R.string.trip_duration_format, trip.duration))

        // Add additional trip information
        durationInfo.append("\nDistance: ${String.format("%.1f", trip.distanceTraveled)} km")
        durationInfo.append("\nAvg Speed: ${String.format("%.1f", trip.averageSpeed)} km/h")
        durationInfo.append("\nFuel Economy: ${String.format("%.1f", trip.averageFuelConsumption)} L/100km")
        durationInfo.append("\nMax RPM: ${trip.maxRPM}")
        durationInfo.append("\nAvg RPM: ${String.format("%.1f", trip.avgRPM)}")

        holder.durationText.text = durationInfo.toString()

        // Set up the delete button click listener to use the callback
        holder.deleteButton.setOnClickListener {
            onDeleteClicked(trip)
        }

        // Set up the whole item click listener to use the callback
        holder.itemView.setOnClickListener {
            onTripClicked(trip)
        }

        // In your ViewHolder or bind method:
        holder.efficiencyScore.text = trip.efficiencyScore.toString()
        // Use the same getScoreColor method for consistent coloring
        holder.efficiencyScore.setTextColor(getScoreColor(trip.efficiencyScore, holder.itemView.context))
    }

    override fun getItemCount() = trips.size

    fun updateTrips(newTrips: List<Trip>) {
        trips.clear()
        trips.addAll(newTrips)
        notifyDataSetChanged()
    }

    private fun getScoreColor(score: Int, context: Context): Int {
        return when {
            score >= 85 -> context.getColor(android.R.color.holo_green_dark)
            score >= 70 -> context.getColor(android.R.color.holo_blue_dark)
            score >= 50 -> context.getColor(android.R.color.holo_orange_dark)
            else -> context.getColor(android.R.color.holo_red_dark)
        }
    }
}