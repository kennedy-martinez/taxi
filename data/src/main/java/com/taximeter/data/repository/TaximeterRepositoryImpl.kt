package com.taximeter.data.repository

import android.util.Log
import com.taximeter.data.network.ApiService
import com.taximeter.data.network.dto.toDomain
import com.taximeter.domain.model.ExecutionConfiguration
import com.taximeter.domain.model.LocationPoint
import com.taximeter.domain.model.PriceConfig
import com.taximeter.domain.model.RouteItem
import com.taximeter.domain.repository.TaximeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaximeterRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : TaximeterRepository {

    override fun getPriceConfig(): Flow<PriceConfig> = flow {
        try {
            Log.d("TaximeterRepo", "Fetching config from API...")
            val configDto = apiService.getPriceConfig()
            emit(configDto.toDomain())
            Log.d("TaximeterRepo", "Config fetched: ${configDto.toDomain()}")
        } catch (e: Exception) {
            Log.e("TaximeterRepo", "Error fetching config", e)
        }
    }.flowOn(Dispatchers.IO)

    override fun getRideUpdates(
        route: RouteItem,
        config: ExecutionConfiguration
    ): Flow<LocationPoint> {
        throw NotImplementedError("To be implemented")
    }
}