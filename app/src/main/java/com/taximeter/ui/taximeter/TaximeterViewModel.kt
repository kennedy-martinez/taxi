package com.taximeter.ui.taximeter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.taximeter.domain.model.ExecutionConfiguration
import com.taximeter.domain.model.LocationPoint
import com.taximeter.domain.model.PriceConfig
import com.taximeter.domain.model.RouteItem
import com.taximeter.domain.repository.TaximeterRepository
import com.taximeter.domain.strategy.SupplementStrategy
import com.taximeter.ui.truncateTwoDecimals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@HiltViewModel
open class TaximeterViewModel @Inject constructor(
    private val repository: TaximeterRepository,
    private val supplementStrategies: Set<@JvmSuppressWildcards SupplementStrategy>
) : ViewModel() {

    private var routeToTest = RouteItem.Route1
    private val executionConfig = ExecutionConfiguration.Fast

    private val strategyMap: Map<String, SupplementStrategy> =
        supplementStrategies.associateBy { it.id }

    private val _rideStatus = MutableStateFlow(RideStatus.IDLE)
    private val _supplementCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val _isError = MutableStateFlow(false)

    private val _activeRideDetails = _rideStatus.flatMapLatest { status ->
        if (status == RideStatus.ACTIVE) {
            repository.getRideUpdates(routeToTest, executionConfig)
                .scan(emptyList<LocationPoint>()) { acc, point -> acc + point }
                .map { points -> calculateRideDetails(points) }
        } else {
            flowOf(Pair(0.0, 0L))
        }
    }

    private val _finalRideData = MutableStateFlow(Pair(0.0, 0L))

    open val uiState: StateFlow<TaximeterUiState> = combine(
        _rideStatus,
        repository.getPriceConfig(),
        _supplementCounts,
        _activeRideDetails,
        _isError
    ) { status, config, supplements, activeDetails, isError ->

        val state: TaximeterUiState = if (config == null) {
            TaximeterUiState(isLoadingConfig = true, isConfigError = false)
        } else if (isError) {
            TaximeterUiState(isLoadingConfig = false, isConfigError = true)
        } else {
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
        }
        state

    }.onStart {
        loadConfig()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = TaximeterUiState(isLoadingConfig = true)
    )

    init {
        _supplementCounts.value = supplementStrategies.associate { it.id to 0 }
    }

    fun onRetryConfig() {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _isError.value = false
            try {
                repository.fetchPriceConfigIfNeeded()
            } catch (e: Exception) {
                _isError.value = true
            }
        }
    }

    fun setTestRoute(route: RouteItem) {
        routeToTest = route
    }

    private fun createIdleUiState(
        supplementCounts: Map<String, Int>
    ): TaximeterUiState {

        val totalSupplementCost = supplementCounts.entries.sumOf { (id, count) ->
            strategyMap[id]?.calculate(count) ?: 0.0
        }

        val breakdown = mutableListOf<PriceBreakdownItem>()
        if (totalSupplementCost > 0) {
            breakdown.add(
                PriceBreakdownItem(PriceBreakdownConcept.SUPPLEMENTS, totalSupplementCost)
            )
        }

        val supplements = createSupplementUiModels(supplementCounts)

        return TaximeterUiState(
            elapsedTimeSeconds = 0L,
            traveledDistanceKm = 0.0,
            supplements = supplements,
            priceBreakdown = breakdown,
            totalFare = totalSupplementCost,
            rideStatus = RideStatus.IDLE,
            isLoadingConfig = false,
            isConfigError = false
        )
    }

    private fun createActiveUiState(
        priceConfig: PriceConfig,
        distanceKm: Double,
        rideTimeSeconds: Long,
        supplementCounts: Map<String, Int>,
        status: RideStatus
    ): TaximeterUiState {

        val distanceCost = (distanceKm * priceConfig.pricePerKm).truncateTwoDecimals()
        val timeCost = (rideTimeSeconds * priceConfig.pricePerSecond).truncateTwoDecimals()

        val totalSupplementCost = supplementCounts.entries.sumOf { (id, count) ->
            strategyMap[id]?.calculate(count) ?: 0.0
        }.truncateTwoDecimals()

        val totalFare = (distanceCost + timeCost + totalSupplementCost).truncateTwoDecimals()

        val breakdown = mutableListOf<PriceBreakdownItem>()
        if (distanceCost > 0) breakdown.add(
            PriceBreakdownItem(PriceBreakdownConcept.DISTANCE, distanceCost)
        )
        if (timeCost > 0) breakdown.add(
            PriceBreakdownItem(PriceBreakdownConcept.TIME, timeCost)
        )
        if (totalSupplementCost > 0) breakdown.add(
            PriceBreakdownItem(PriceBreakdownConcept.SUPPLEMENTS, totalSupplementCost)
        )

        val supplements = createSupplementUiModels(supplementCounts)

        return TaximeterUiState(
            elapsedTimeSeconds = rideTimeSeconds,
            traveledDistanceKm = distanceKm,
            supplements = supplements,
            priceBreakdown = breakdown,
            totalFare = totalFare,
            rideStatus = status,
            isLoadingConfig = false,
            isConfigError = false
        )
    }

    private fun createSupplementUiModels(supplementCounts: Map<String, Int>): List<SupplementUiModel> {
        return supplementStrategies.map { strategy ->
            SupplementUiModel(
                id = strategy.id,
                count = supplementCounts.getOrDefault(strategy.id, 0)
            )
        }
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
        return Pair((totalDistanceMeters / 1000.0).truncateTwoDecimals(), totalTimeSeconds)
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
                _supplementCounts.value = supplementStrategies.associate { it.id to 0 }
            }
        }
    }

    fun onSupplementChange(supplementId: String, change: Int) {
        if (_rideStatus.value == RideStatus.FINISHED) return

        _supplementCounts.update { currentCounts ->
            val newMap = currentCounts.toMutableMap()
            val currentCount = newMap.getOrDefault(supplementId, 0)
            val newCount = maxOf(0, currentCount + change)
            newMap[supplementId] = newCount
            newMap
        }
    }
}