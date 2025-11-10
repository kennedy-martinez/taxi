package com.taximeter.ui.taximeter

enum class RideStatus {
    IDLE,
    ACTIVE,
    FINISHED
}

data class SupplementUiModel(
    val id: String,
    val count: Int
)

data class PriceBreakdownItem(
    val concept: PriceBreakdownConcept,
    val price: Double
)

data class TaximeterUiState(
    val elapsedTimeSeconds: Long = 0L,
    val traveledDistanceKm: Double = 0.0,
    val supplements: List<SupplementUiModel> = emptyList(),
    val priceBreakdown: List<PriceBreakdownItem> = emptyList(),
    val totalFare: Double = 0.0,
    val rideStatus: RideStatus = RideStatus.IDLE,
    val isLoadingConfig: Boolean = true,
    val isConfigError: Boolean = false
)