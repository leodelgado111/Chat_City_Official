package com.chatcityofficial.chatmapapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chatcityofficial.chatmapapp.R
import com.google.android.libraries.places.api.model.AutocompletePrediction

class PlaceSearchAdapter(
    private val onPlaceSelected: (AutocompletePrediction) -> Unit
) : ListAdapter<AutocompletePrediction, PlaceSearchAdapter.PlaceViewHolder>(PlaceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_search, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        val place = getItem(position)
        holder.bind(place)
    }

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val placeNameTextView: TextView = itemView.findViewById(R.id.placeNameTextView)
        private val placeAddressTextView: TextView = itemView.findViewById(R.id.placeAddressTextView)

        fun bind(prediction: AutocompletePrediction) {
            placeNameTextView.text = prediction.getPrimaryText(null).toString()
            placeAddressTextView.text = prediction.getSecondaryText(null).toString()
            
            itemView.setOnClickListener {
                onPlaceSelected(prediction)
            }
        }
    }

    class PlaceDiffCallback : DiffUtil.ItemCallback<AutocompletePrediction>() {
        override fun areItemsTheSame(
            oldItem: AutocompletePrediction,
            newItem: AutocompletePrediction
        ): Boolean {
            return oldItem.placeId == newItem.placeId
        }

        override fun areContentsTheSame(
            oldItem: AutocompletePrediction,
            newItem: AutocompletePrediction
        ): Boolean {
            return oldItem == newItem
        }
    }
}
