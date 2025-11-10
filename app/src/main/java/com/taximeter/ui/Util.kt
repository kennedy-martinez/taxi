package com.taximeter.ui


fun Double.truncateTwoDecimals(): Double {
    return kotlin.math.floor(this * 100) / 100
}