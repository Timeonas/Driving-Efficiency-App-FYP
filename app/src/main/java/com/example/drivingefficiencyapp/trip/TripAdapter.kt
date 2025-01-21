package com.example.drivingefficiencyapp.trip

/**
 * Adapter for translating the data to how it is displayed in the RecyclerView.
 *
 * @author Tim Samoska
 * @since January 17, 2025
 */

import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.example.drivingefficiencyapp.R
import com.example.drivingefficiencyapp.databinding.TripItemBinding

class TripAdapter(private val trips: List<Trip>) :
    //Adapter for the RecyclerView, takes in a list of trips as a parameter
    RecyclerView.Adapter<TripAdapter.TripViewHolder>() {
        /**
         * ViewHolder class for holding the views of a single trip item.
         *
         * @property binding The view binding for the trip item layout
         */
        class TripViewHolder(private val binding: TripItemBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(trip: Trip) {
                binding.tripDate.text = trip.date
                binding.tripDuration.text = binding.root.context.getString(R.string.trip_duration_format, trip.duration)
            }
        }

        /**
         * Creates a new ViewHolder for the RecyclerView.
         *
         * @param parent The parent view group
         * @param viewType The type of view
         * @return A new TripViewHolder
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
            val binding = TripItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return TripViewHolder(binding)
        }

        /**
         * Binds trip data to the ViewHolder at the specified position.
         *
         * @param holder The ViewHolder to bind data to
         * @param position The position of the item in the trips list
         */
        override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
            holder.bind(trips[position])
        }

        /**
         * Gets the number of items in the list of trips.
         *
         * @return The number of trips in the list
         */
        override fun getItemCount() = trips.size
    }