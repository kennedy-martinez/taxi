package com.taximeter.data.strategy

import com.taximeter.domain.strategy.SupplementStrategy
import javax.inject.Inject

class LuggageStrategy @Inject constructor() : SupplementStrategy {

    override val id: String = "luggage"

    override fun calculate(count: Int): Double {
        return count * LUGGAGE_COST_EUR
    }

    private companion object {
        private const val LUGGAGE_COST_EUR = 5.0
    }
}