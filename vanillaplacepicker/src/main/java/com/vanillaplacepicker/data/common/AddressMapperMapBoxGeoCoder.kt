package com.vanillaplacepicker.data.common

import com.vanillaplacepicker.data.MapBoxGeoCoderAddressResponse
import com.vanillaplacepicker.data.VanillaAddress

object AddressMapperMapBoxGeoCoder: BaseMapper<MapBoxGeoCoderAddressResponse.Features, VanillaAddress>(){

    override fun map(oldItem: MapBoxGeoCoderAddressResponse.Features): VanillaAddress {
        return VanillaAddress().apply {
            this.formattedAddress = oldItem.place_name
            this.name = oldItem.text
            this.placeId = oldItem.id
            this.longitude = oldItem.center!![0]
            this.latitude = oldItem.center!![1]
        }
    }

}