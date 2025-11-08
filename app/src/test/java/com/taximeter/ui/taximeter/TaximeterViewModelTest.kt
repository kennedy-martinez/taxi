package com.taximeter.ui.taximeter

import app.cash.turbine.test
import com.taximeter.domain.model.ExecutionConfiguration
import com.taximeter.domain.model.LocationPoint
import com.taximeter.domain.model.PriceConfig
import com.taximeter.domain.model.RouteItem
import com.taximeter.domain.repository.TaximeterRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class TaximeterViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: TaximeterRepository
    private lateinit var viewModel: TaximeterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        coEvery { repository.getPriceConfig() } returns flowOf(TEST_PRICE_CONFIG)
        coEvery { repository.fetchPriceConfigIfNeeded() } returns Unit
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GIVEN test scenario WHEN 1 luggage added and ride starts THEN totalFare is 25,20`() =
        runTest {

            val fakeRideFlow = flowOf(TEST_START_POINT, TEST_END_POINT)

            coEvery {
                repository.getRideUpdates(TEST_ROUTE, TEST_EXECUTION_CONFIG)
            } returns fakeRideFlow

            viewModel = TaximeterViewModel(repository)

            viewModel.uiState.test {

                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()

                viewModel.onSupplementChange(TEST_SUPPLEMENT_ID, 1)
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()

                viewModel.onStartStopClick()
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()

                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
                awaitItem()

                viewModel.onStartStopClick()
                testDispatcher.scheduler.advanceUntilIdle()

                val finalState = awaitItem()

                assertEquals(RideStatus.FINISHED, finalState.rideStatus)
                assertEquals(EXPECTED_DISTANCE_STRING, finalState.traveledDistance)
                assertEquals(EXPECTED_TIME_STRING, finalState.elapsedTime)
                assertEquals(EXPECTED_FARE_STRING_FINAL, finalState.totalFare.replace(',', '.'))
            }
        }

    companion object {
        private const val TEST_SUPPLEMENT_ID = "luggage"
        private const val TEST_PRICE_PER_KM = 0.2
        private const val TEST_PRICE_PER_SECOND = 0.01
        private const val TEST_LUGGAGE_COST = 5.0
        private const val TEST_DISTANCE_KM = 11.0
        private const val TEST_DURATION_SECONDS = 1800
        private const val EXPECTED_FINAL_FARE_DOUBLE = (TEST_DISTANCE_KM * TEST_PRICE_PER_KM) +
                (TEST_DURATION_SECONDS * TEST_PRICE_PER_SECOND) +
                TEST_LUGGAGE_COST

        private val EXPECTED_DISTANCE_STRING = String.format(Locale.US, "%.1f km", TEST_DISTANCE_KM)
        private val EXPECTED_TIME_STRING = String.format(Locale.US, "%02d:%02d:%02d", 0, 30, 0)
        private val EXPECTED_FARE_STRING_ONE_LUGGAGE =
            String.format(Locale.US, "%.2f €", TEST_LUGGAGE_COST)
        private val EXPECTED_FARE_STRING_FINAL =
            String.format(Locale.US, "%.2f €", EXPECTED_FINAL_FARE_DOUBLE)

        private val TEST_PRICE_CONFIG = PriceConfig(
            pricePerKm = TEST_PRICE_PER_KM,
            pricePerSecond = TEST_PRICE_PER_SECOND
        )
        private const val TEST_START_LATITUDE = 40.0
        private const val TEST_START_LONGITUDE = -3.0
        private const val TEST_END_LATITUDE = 40.099099
        private const val TEST_END_LONGITUDE = -3.0

        private val TEST_START_TIME = System.currentTimeMillis()
        private val TEST_END_TIME =
            TEST_START_TIME + TimeUnit.SECONDS.toMillis(TEST_DURATION_SECONDS.toLong())

        private val TEST_START_POINT = LocationPoint(
            latitude = TEST_START_LATITUDE,
            longitude = TEST_START_LONGITUDE,
            timestamp = TEST_START_TIME
        )
        private val TEST_END_POINT = LocationPoint(
            latitude = TEST_END_LATITUDE,
            longitude = TEST_END_LONGITUDE,
            timestamp = TEST_END_TIME
        )

        private val TEST_ROUTE = RouteItem.Route1
        private val TEST_EXECUTION_CONFIG = ExecutionConfiguration.Fast
    }
}