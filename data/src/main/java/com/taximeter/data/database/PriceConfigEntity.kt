package com.taximeter.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.taximeter.data.network.dto.PriceConfigDto
import com.taximeter.domain.model.PriceConfig

@Entity(tableName = "price_config")
data class PriceConfigEntity(
    @PrimaryKey val id: Int = 1,
    val pricePerKm: Double,
    val pricePerSecond: Double
)

fun PriceConfigEntity.toDomain(): PriceConfig {
    return PriceConfig(
        pricePerKm = this.pricePerKm,
        pricePerSecond = this.pricePerSecond
    )
}

fun PriceConfigDto.toEntity(): PriceConfigEntity {
    return PriceConfigEntity(
        pricePerKm = this.pricePerKm,
        pricePerSecond = this.pricePerSecond
    )
}