package com.chatcityofficial.chatmapapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.chatcityofficial.chatmapapp.R
import com.google.android.libraries.places.api.model.AutocompletePrediction

class PlaceSearchAdapter(
    private val onPlaceSelected: (AutocompletePrediction) -> Unit
) : RecyclerView.Adapter<PlaceSearchAdapter.PlaceViewHolder>() {

    private var predictions = listOf<AutocompletePrediction>()

    fun updatePredictions(newPredictions: List<AutocompletePrediction>) {
        predictions = newPredictions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_place_search, parent, false)
        return PlaceViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaceViewHolder, position: Int) {
        holder.bind(predictions[position])
    }

    override fun getItemCount() = predictions.size

    inner class PlaceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val primaryText: TextView = itemView.findViewById(R.id.primaryText)
        private val secondaryText: TextView = itemView.findViewById(R.id.secondaryText)

        fun bind(prediction: AutocompletePrediction) {
            primaryText.text = prediction.getPrimaryText(null).toString()
            secondaryText.text = prediction.getSecondaryText(null).toString()
            
            itemView.setOnClickListener {
                onPlaceSelected(prediction)
            }
        }
    }
}