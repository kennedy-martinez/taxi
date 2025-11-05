package com.taximeter.ui.taximeter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taximeter.domain.model.ExecutionConfiguration
import com.taximeter.domain.model.LocationPoint
import com.taximeter.domain.model.PriceConfig
import com.taximeter.domain.model.RouteItem
import com.taximeter.domain.repository.TaximeterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@HiltViewModel
class TaximeterViewModel @Inject constructor(
    private val repository: TaximeterRepository
) : ViewModel() {

    private val routeToTest = RouteItem.Route1
    private val executionConfig = ExecutionConfiguration.Fast

    private val _rideStatus = MutableStateFlow(RideStatus.IDLE)
    private val _supplementCounts = MutableStateFlow<Map<String, Int>>(emptyMap())

    private val _activeRideDetails = _rideStatus.flatMapLatest { status ->
        if (status == RideStatus.ACTIVE) {
            repository.getRideUpdates(routeToTest, executionConfig)
                .scan(emptyList<LocationPoint>()) { acc, point -> acc + point }
                .map { points -> calculateRideDetails(points) }
                .onCompletion { cause ->
                    if (cause == null) {
                        _rideStatus.value = RideStatus.FINISHED
                    }
                }
        } else {
            flowOf(Pair(0.0, 0L))
        }
    }

    private val _finalRideData = MutableStateFlow(Pair(0.0, 0L))

    val uiState: StateFlow<TaximeterUiState> = combine(
        _rideStatus,
        repository.getPriceConfig().filterNotNull(),
        _supplementCounts,
        _activeRideDetails
    ) { status, config, supplements, activeDetails ->

        if (status == RideStatus.ACTIVE) {
            _finalRideData.value = activeDetails
        }

        when (status) {
            RideStatus.IDLE -> createIdleUiState(supplements)

            RideStatus.ACTIVE -> {
                val (dist, timeGps) = activeDetails
                createActiveUiState(config, dist, timeGps, supplements, RideStatus.ACTIVE)
            }

            RideStatus.FINISHED -> {
                val (dist, timeGps) = _finalRideData.value
                createActiveUiState(config, dist, timeGps, supplements, RideStatus.FINISHED)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaximeterUiState()
    )

    init {
        viewModelScope.launch {
            repository.fetchPriceConfigIfNeeded()
        }
        _supplementCounts.value = mapOf(
            "luggage" to 0,
            "supplement_2" to 0
        )
    }

    private fun createIdleUiState(
        supplementCounts: Map<String, Int>
    ): TaximeterUiState {
        val supplements = listOf(
            SupplementUiModel("luggage", "Supplement 1", supplementCounts.getOrDefault("luggage", 0)),
            SupplementUiModel("supplement_2", "Supplement 2", supplementCounts.getOrDefault("supplement_2", 0))
        )
        val luggageCost = supplementCounts.getOrDefault("luggage", 0) * 5.0
        val totalSupplementCost = luggageCost

        val breakdown = mutableListOf<PriceBreakdownItem>()
        if (luggageCost > 0) {
            breakdown.add(PriceBreakdownItem("Supplements", "%.2f €".format(totalSupplementCost)))
        }

        return TaximeterUiState(
            elapsedTime = "00:00:00",
            traveledDistance = "0.0 km",
            supplements = supplements,
            priceBreakdown = breakdown,
            totalFare = "%.2f €".format(totalSupplementCost),
            rideStatus = RideStatus.IDLE
        )
    }

    private fun createActiveUiState(
        priceConfig: PriceConfig,
        distanceKm: Double,
        rideTimeSeconds: Long,
        supplementCounts: Map<String, Int>,
        status: RideStatus
    ): TaximeterUiState {

        val distanceCost = distanceKm * priceConfig.pricePerKm
        val timeCost = rideTimeSeconds * priceConfig.pricePerSecond
        val luggageCost = supplementCounts.getOrDefault("luggage", 0) * 5.0

        val totalSupplementCost = luggageCost
        val totalFare = distanceCost + timeCost + totalSupplementCost

        val breakdown = mutableListOf<PriceBreakdownItem>()
        if (distanceCost > 0) breakdown.add(PriceBreakdownItem("Distance", "%.2f €".format(distanceCost)))
        if (timeCost > 0) breakdown.add(PriceBreakdownItem("Time", "%.2f €".format(timeCost)))
        if (totalSupplementCost > 0) breakdown.add(PriceBreakdownItem("Supplements", "%.2f €".format(totalSupplementCost)))


        val supplements = listOf(
            SupplementUiModel("luggage", "Supplement 1", supplementCounts.getOrDefault("luggage", 0)),
            SupplementUiModel("supplement_2", "Supplement 2", supplementCounts.getOrDefault("supplement_2", 0))
        )

        val hours = TimeUnit.SECONDS.toHours(rideTimeSeconds)
        val minutes = TimeUnit.SECONDS.toMinutes(rideTimeSeconds) % 60
        val seconds = rideTimeSeconds % 60
        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        return TaximeterUiState(
            elapsedTime = timeString,
            traveledDistance = "%.1f km".format(distanceKm),
            supplements = supplements,
            priceBreakdown = breakdown,
            totalFare = "%.2f €".format(totalFare),
            rideStatus = status
        )
    }

    private fun calculateRideDetails(points: List<LocationPoint>): Pair<Double, Long> {
        if (points.isEmpty() || points.size == 1) {
            return Pair(0.0, 0L)
        }
        val totalDistanceMeters = points.zipWithNext { a, b ->
            haversineDistance(a.latitude, a.longitude, b.latitude, b.longitude)
        }.sum()
        val startTime = points.first().timestamp
        val endTime = points.last().timestamp
        val totalTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(endTime - startTime)
        return Pair(totalDistanceMeters / 1000.0, totalTimeSeconds)
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val a = sin(deltaPhi / 2) * sin(deltaPhi / 2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2) * sin(deltaLambda / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun onStartStopClick() {
        when (_rideStatus.value) {
            RideStatus.IDLE -> {
                _rideStatus.value = RideStatus.ACTIVE
            }
            RideStatus.ACTIVE -> {
                _rideStatus.value = RideStatus.FINISHED
            }
            RideStatus.FINISHED -> {
                _rideStatus.value = RideStatus.IDLE
                _finalRideData.value = Pair(0.0, 0L)
                _supplementCounts.value = mapOf("luggage" to 0, "supplement_2" to 0)
            }
        }
    }

    fun onSupplementChange(supplementId: String, change: Int) {
        if (_rideStatus.value == RideStatus.ACTIVE) return

        _supplementCounts.update { currentCounts ->
            val newMap = currentCounts.toMutableMap()
            val currentCount = newMap.getOrDefault(supplementId, 0)
            val newCount = maxOf(0, currentCount + change)
            newMap[supplementId] = newCount
            newMap
        }
    }
}