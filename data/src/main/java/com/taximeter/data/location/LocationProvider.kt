package com.taximeter.data.location

import com.taximeter.data.location.LocationProvider.LocationPoint
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

    private val route1 = listOf(
        LocationPoint(
            40.416775,
            -3.703790,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(1)
        ),
        LocationPoint(
            40.417775,
            -3.703790,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10)
        ),
        LocationPoint(
            40.418775,
            -3.703790,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20)
        ),
        LocationPoint(
            40.419775,
            -3.703790,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30)
        ),
        LocationPoint(
            40.420775,
            -3.703790,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(40)
        ),
        LocationPoint(
            40.421775,
            -3.703790,
            System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(50)
        )
    )

    fun getRouteFlow(
        routeItem: RouteItem,
        executionConfiguration: ExecutionConfiguration
    ): Flow<LocationPoint> {

        val route = when (routeItem) {
            RouteItem.Route1 -> route1
            RouteItem.Route2 -> route1.reversed()
            RouteItem.Route3 -> route1.take(3)
        }

        val delayMs = when (executionConfiguration) {
            ExecutionConfiguration.Default -> 1000L
            ExecutionConfiguration.Fast -> 10L
        }

        return flow {
            for (point in route) {
                emit(point)
                delay(delayMs)
            }
        }
    }
}

fun LocationPoint.toDomain(): DomainLocationPoint {
    return DomainLocationPoint(
        latitude = this.latitude,
        longitude = this.longitude,
        timestamp = this.timestamp
    )
}