package com.vanillaplacepicker.presentation.mapbox.autocomplete

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.vanillaplacepicker.R
import com.vanillaplacepicker.data.MapBoxGeoCoderAddressResponse
import com.vanillaplacepicker.extenstion.inflate
import kotlinx.android.synthetic.main.row_auto_complete_place.view.*

class VanillaMapBoxAutoCompleteAdapter(private val onItemSelected: (data: MapBoxGeoCoderAddressResponse.Features) -> Unit):
    RecyclerView.Adapter<VanillaMapBoxAutoCompleteAdapter.MiAutoCompleteViewHolder>() {

    var placeList = ArrayList<MapBoxGeoCoderAddressResponse.Features>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiAutoCompleteViewHolder {
        return MiAutoCompleteViewHolder(parent.inflate(R.layout.row_auto_complete_place))
    }

    fun setList(mArrayList: ArrayList<MapBoxGeoCoderAddressResponse.Features>) {
        this.placeList.clear()
        this.placeList.addAll(mArrayList)
        notifyDataSetChanged()
    }

    fun clearList() {
        this.placeList.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return placeList.size
    }

    override fun onBindViewHolder(holder: MiAutoCompleteViewHolder, position: Int) {
        holder.bind(placeList[position])
    }

    inner class MiAutoCompleteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(predictions: MapBoxGeoCoderAddressResponse.Features) {
            itemView.tvPlaceName.text = predictions.place_name
            itemView.tvPlaceAddress.text = predictions.text
            itemView.setOnClickListener {
                onItemSelected(predictions)
            }
        }
    }
}