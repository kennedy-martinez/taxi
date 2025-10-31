package com.taximeter.data.repository

import android.util.Log
import com.taximeter.data.database.PriceConfigDao
import com.taximeter.data.database.toDomain
import com.taximeter.data.database.toEntity
import com.taximeter.data.location.LocationProvider
import com.taximeter.data.location.toDomain
import com.taximeter.data.network.ApiService
import com.taximeter.domain.model.ExecutionConfiguration
import com.taximeter.domain.model.LocationPoint
import com.taximeter.domain.model.PriceConfig
import com.taximeter.domain.model.RouteItem
import com.taximeter.domain.repository.TaximeterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaximeterRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val configDao: PriceConfigDao,
    private val locationProvider: LocationProvider
) : TaximeterRepository {

    override fun getPriceConfig(): Flow<PriceConfig?> {
        return configDao.getConfigFlow()
            .map { entity ->
                entity?.toDomain()
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun fetchPriceConfigIfNeeded() {
        try {
            val isCacheEmpty = configDao.getConfigCount() == 0
            if (isCacheEmpty) {
                Log.d("TaximeterRepo", "Caché vacía. Obteniendo de la API...")
                val configDto = apiService.getPriceConfig()
                configDao.insertConfig(configDto.toEntity())
                Log.d("TaximeterRepo", "Configuración guardada en caché.")
            } else {
                Log.d("TaximeterRepo", "Configuración cargada desde caché.")
            }
        } catch (e: Exception) {
            Log.e("TaximeterRepo", "Error al refrescar la configuración", e)
        }
    }

    override fun getRideUpdates(
        route: RouteItem,
        config: ExecutionConfiguration
    ): Flow<LocationPoint> {
        val providerRoute = LocationProvider.RouteItem.valueOf(route.name)
        val providerConfig = LocationProvider.ExecutionConfiguration.valueOf(config.name)

        return locationProvider.getRouteFlow(providerRoute, providerConfig)
            .map { locationPointData ->
                locationPointData.toDomain()
            }
            .flowOn(Dispatchers.IO)
    }
}