package com.example.drivingefficiencyapp

/**
 * Adapter for translating the data to how it is displayed in the RecyclerView.
 *
 * @author Tim Samoska
 * @since January 17, 2025
 */

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TripAdapter(private val trips: List<Trip>) :
    //Adapter for the RecyclerView, takes in a list of trips as a parameter
    RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

        /**
         * ViewHolder class for holding the views of a single trip item.
         *
         * @property dateText TextView that displays the trip date
         * @property durationText TextView that displays the trip duration
         */
        class TripViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            //TextViews for the date and duration of the trip from the stored trip_item.xml layout
            val dateText: TextView = itemView.findViewById(R.id.tripDate)
            val durationText: TextView = itemView.findViewById(R.id.tripDuration)
        }

        /**
         * Creates a new ViewHolder for the RecyclerView.
         *
         * @param parent The parent view group
         * @param viewType The type of view
         * @return A new TripViewHolder
         */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
            //Inflates the trip_item.xml layout for each item in the RecyclerView
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.trip_item, parent, false)
            //Returns a new TripViewHolder with the inflated view
            return TripViewHolder(view)
        }

        /**
         * Binds trip data to the ViewHolder at the specified position.
         *
         * @param holder The ViewHolder to bind data to
         * @param position The position of the item in the trips list
         */
        override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
            //Parse the trip data and set the date and duration in the TextViews
            val trip = trips[position]
            holder.dateText.text = trip.date
            holder.durationText.text = "Duration: ${trip.duration}"
        }

        /**
         * Gets the number of items in the list of trips.
         *
         * @return The number of trips in the list
         */
        override fun getItemCount() = trips.size
    }