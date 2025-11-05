package com.taximeter.data.location

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.TimeUnit
import com.taximeter.domain.model.LocationPoint as DomainLocationPoint

class LocationProvider {

    data class LocationPoint(
        val latitude: Double,
        val longitude: Double,
        val timestamp: Long
    )

    enum class RouteItem { Route1, Route2, Route3 }
    enum class ExecutionConfiguration { Default, Fast }

    private val shortRoute: List<LocationPoint> by lazy { generateShortRoute() }

    fun getRouteFlow(
        routeItem: RouteItem,
        executionConfiguration: ExecutionConfiguration
    ): Flow<LocationPoint> {

        val delayMs = when (executionConfiguration) {
            ExecutionConfiguration.Default -> EMISSION_DELAY_DEFAULT_MS
            ExecutionConfiguration.Fast -> EMISSION_DELAY_FAST_MS
        }

        return when (routeItem) {
            RouteItem.Route1 -> generateExampleScenario(delayMs)
            RouteItem.Route2 -> createRouteFlow(shortRoute, delayMs)
            RouteItem.Route3 -> createRouteFlow(shortRoute.reversed(), delayMs)
        }
    }

    private fun generateExampleScenario(delayMs: Long): Flow<LocationPoint> = flow {
        val startTime = System.currentTimeMillis()
        for (i in 0..EXAMPLE_ROUTE_DURATION_IN_SECONDS) {
            val currentLat = BASE_STARTING_LATITUDE + (i * EXAMPLE_ROUTE_LATITUDE_STEP_PER_SECOND)
            val currentTimestamp = startTime + (i * MILLISECONDS_PER_SECOND)

            emit(
                LocationPoint(
                    latitude = currentLat,
                    longitude = BASE_STARTING_LONGITUDE,
                    timestamp = currentTimestamp
                )
            )
            delay(delayMs)
        }
    }

    private fun generateShortRoute(): List<LocationPoint> {
        val points = mutableListOf<LocationPoint>()
        val startTime = System.currentTimeMillis()
        for (i in 0 until SHORT_ROUTE_TOTAL_POINTS) {
            val timestamp = if (i == 0) {
                startTime + TimeUnit.SECONDS.toMillis(SHORT_ROUTE_FIRST_POINT_DELAY_IN_SECONDS)
            } else {
                startTime + (i * TimeUnit.SECONDS.toMillis(SHORT_ROUTE_TIMESTAMP_INTERVAL_IN_SECONDS))
            }
            points.add(
                LocationPoint(
                    latitude = BASE_STARTING_LATITUDE + (i * SHORT_ROUTE_LATITUDE_STEP_PER_POINT),
                    longitude = BASE_STARTING_LONGITUDE,
                    timestamp = timestamp
                )
            )
        }
        return points
    }

    private fun createRouteFlow(points: List<LocationPoint>, delayMs: Long): Flow<LocationPoint> =
        flow {
            for (point in points) {
                emit(point)
                delay(delayMs)
            }
        }

    companion object {
        private const val MILLISECONDS_PER_SECOND = 1000L

        private const val EMISSION_DELAY_DEFAULT_MS = 1000L
        private const val EMISSION_DELAY_FAST_MS = 10L

        private const val BASE_STARTING_LATITUDE = 40.416775
        private const val BASE_STARTING_LONGITUDE = -3.703790

        private const val EXAMPLE_ROUTE_DURATION_IN_SECONDS = 1800
        private const val EXAMPLE_ROUTE_LATITUDE_STEP_PER_SECOND = 0.000055

        private const val SHORT_ROUTE_TOTAL_POINTS = 6
        private const val SHORT_ROUTE_LATITUDE_STEP_PER_POINT = 0.001
        private const val SHORT_ROUTE_FIRST_POINT_DELAY_IN_SECONDS = 1L
        private const val SHORT_ROUTE_TIMESTAMP_INTERVAL_IN_SECONDS = 10L
    }
}

fun LocationProvider.LocationPoint.toDomain(): DomainLocationPoint {
    return DomainLocationPoint(
        latitude = this.latitude,
        longitude = this.longitude,
        timestamp = this.timestamp
    )
}