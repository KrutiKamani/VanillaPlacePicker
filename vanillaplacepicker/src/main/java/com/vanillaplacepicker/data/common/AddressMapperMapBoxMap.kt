package com.vanillaplacepicker.data.common

import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.vanillaplacepicker.data.VanillaAddress

object AddressMapperMapBoxMap : BaseMapper<CarmenFeature, VanillaAddress>(){

    override fun map(oldItem: CarmenFeature): VanillaAddress {
        return VanillaAddress().apply {
            this.formattedAddress = oldItem.placeName()
            this.name = oldItem.text()
            this.placeId = oldItem.id()
            this.latitude = oldItem.center()?.latitude()
            this.longitude = oldItem.center()?.longitude()
        }
    }

}