package com.example.drivingefficiencyapp.trip

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.drivingefficiencyapp.R

class TripAdapter(
    val trips: MutableList<Trip> = mutableListOf(),
    private val onDeleteClicked: (Trip) -> Unit,
    private val onTripClicked: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.tripDate)
        val durationText: TextView = itemView.findViewById(R.id.tripDuration)
        val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
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

        holder.durationText.text = durationInfo.toString()

        holder.deleteButton.setOnClickListener {
            onDeleteClicked(trip)
        }

        holder.itemView.setOnClickListener {
            onTripClicked(trip)
        }
    }

    override fun getItemCount() = trips.size

    fun updateTrips(newTrips: List<Trip>) {
        trips.clear()
        trips.addAll(newTrips)
        notifyDataSetChanged()
    }

    fun removeTrip(position: Int) {
        if (position in trips.indices) {
            trips.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}