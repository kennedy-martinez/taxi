package com.taximeter.domain.strategy

interface SupplementStrategy {
    val id: String
    fun calculate(count: Int): Double
}