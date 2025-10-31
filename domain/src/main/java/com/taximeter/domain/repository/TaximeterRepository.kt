package com.taximeter.domain.repository

import com.taximeter.domain.model.ExecutionConfiguration
import com.taximeter.domain.model.LocationPoint
import com.taximeter.domain.model.PriceConfig
import com.taximeter.domain.model.RouteItem
import kotlinx.coroutines.flow.Flow

interface TaximeterRepository {

    fun getPriceConfig(): Flow<PriceConfig>

    fun getRideUpdates(
        route: RouteItem,
        config: ExecutionConfiguration
    ): Flow<LocationPoint>
}