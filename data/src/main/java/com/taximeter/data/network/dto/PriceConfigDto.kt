package com.taximeter.data.network.dto


import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.taximeter.domain.model.PriceConfig

@JsonClass(generateAdapter = true)
data class PriceConfigDto(
    @Json(name = "price_per_km") val pricePerKm: Double,
    @Json(name = "price_per_second") val pricePerSecond: Double
)

fun PriceConfigDto.toDomain(): PriceConfig {
    return PriceConfig(
        pricePerKm = this.pricePerKm,
        pricePerSecond = this.pricePerSecond
    )
}