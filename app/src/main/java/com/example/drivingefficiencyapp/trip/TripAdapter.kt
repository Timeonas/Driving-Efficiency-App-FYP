package com.example.drivingefficiencyapp.trip

import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.databinding.TripItemBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class TripAdapter(
    private val trips: List<Trip>,
    private val onDeleteTrip: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(private val binding: TripItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(trip: Trip) {
            binding.tripDate.text = trip.date
            binding.tripDuration.text = binding.root.context.getString(
                R.string.trip_duration_format,
                trip.duration
            )

            // Set delete button click listener
            binding.deleteButton.setOnClickListener {
                showDeleteDialog(it, trip)
            }
        }

        private fun showDeleteDialog(view: View, trip: Trip) {
            MaterialAlertDialogBuilder(view.context)
                .setTitle("Delete Trip")
                .setMessage("Are you sure you want to delete this trip?")
                .setPositiveButton("Delete") { _, _ ->
                    onDeleteTrip(trip)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = TripItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(trips[position])
    }

    override fun getItemCount() = trips.size
}