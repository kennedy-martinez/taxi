package com.taximeter.ui.taximeter

enum class RideStatus {
    IDLE,
    ACTIVE,
    FINISHED
}

data class SupplementUiModel(
    val id: String,
    val name: String,
    val count: Int
)

data class PriceBreakdownItem(
    val concept: String,
    val price: String
)

data class TaximeterUiState(
    val elapsedTime: String = "00:00:00",
    val traveledDistance: String = "0.0 km",
    val supplements: List<SupplementUiModel> = emptyList(),
    val priceBreakdown: List<PriceBreakdownItem> = emptyList(),
    val totalFare: String = "0.00 €",
    val rideStatus: RideStatus = RideStatus.IDLE,
    val isLoadingConfig: Boolean = true,
    val isConfigError: Boolean = false
)