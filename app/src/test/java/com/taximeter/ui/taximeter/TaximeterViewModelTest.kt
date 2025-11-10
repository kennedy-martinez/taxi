package com.taximeter.ui.taximeter

import app.cash.turbine.test
import com.taximeter.domain.model.ExecutionConfiguration
import com.taximeter.domain.model.LocationPoint
import com.taximeter.domain.model.PriceConfig
import com.taximeter.domain.model.RouteItem
import com.taximeter.domain.repository.TaximeterRepository
import com.taximeter.domain.strategy.SupplementStrategy
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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

    private lateinit var mockStrategiesSet: Set<SupplementStrategy>
    private lateinit var mockLuggageStrategy: SupplementStrategy

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        mockLuggageStrategy = mockk(relaxed = true)

        coEvery { repository.getPriceConfig() } returns flowOf(TEST_PRICE_CONFIG)
        coEvery { repository.fetchPriceConfigIfNeeded() } returns Unit

        every { mockLuggageStrategy.id } returns TEST_SUPPLEMENT_ID
        every { mockLuggageStrategy.calculate(any()) } answers {
            firstArg<Int>() * TEST_LUGGAGE_COST
        }
        mockStrategiesSet = setOf(mockLuggageStrategy)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `GXIVEN test scenario WHEN 1 luggage added and ride starts THEN totalFare is 25,20`() =
        runTest {

            val testStartTime = testDispatcher.scheduler.currentTime
            val testEndTime =
                testStartTime + TimeUnit.SECONDS.toMillis(TEST_DURATION_SECONDS.toLong())
            val testStartPoint = LocationPoint(
                latitude = TEST_START_LATITUDE,
                longitude = TEST_START_LONGITUDE,
                timestamp = testStartTime
            )
            val testEndPoint = LocationPoint(
                latitude = TEST_END_LATITUDE,
                longitude = TEST_END_LONGITUDE,
                timestamp = testEndTime
            )

            val fakeRideFlow = flowOf(testStartPoint, testEndPoint)

            coEvery {
                repository.getRideUpdates(TEST_ROUTE, TEST_EXECUTION_CONFIG)
            } returns fakeRideFlow

            viewModel = TaximeterViewModel(repository, mockStrategiesSet)

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

    @Test
    fun `GIVEN config is loading WHEN vm starts THEN shows loading state`() = runTest {
        coEvery { repository.getPriceConfig() } returns flowOf(null)
        coEvery { repository.fetchPriceConfigIfNeeded() } coAnswers {
            kotlinx.coroutines.delay(5000)
        }
        viewModel = TaximeterViewModel(repository, mockStrategiesSet)
        viewModel.uiState.test {
            val loadingState = awaitItem()
            assertEquals(true, loadingState.isLoadingConfig)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `GIVEN test scenario WHEN 0 luggage added and ride starts THEN totalFare is 20,20`() =
        runTest {

            val testStartTime = testDispatcher.scheduler.currentTime
            val testEndTime =
                testStartTime + TimeUnit.SECONDS.toMillis(TEST_DURATION_SECONDS.toLong())
            val testStartPoint = LocationPoint(
                latitude = TEST_START_LATITUDE,
                longitude = TEST_START_LONGITUDE,
                timestamp = testStartTime
            )
            val testEndPoint = LocationPoint(
                latitude = TEST_END_LATITUDE,
                longitude = TEST_END_LONGITUDE,
                timestamp = testEndTime
            )

            val fakeRideFlow = flowOf(testStartPoint, testEndPoint)

            coEvery {
                repository.getRideUpdates(
                    TEST_ROUTE,
                    TEST_EXECUTION_CONFIG
                )
            } returns fakeRideFlow

            viewModel = TaximeterViewModel(repository, mockStrategiesSet)
            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
                viewModel.onStartStopClick()
                runCurrent()
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
                awaitItem()
                viewModel.onStartStopClick()
                runCurrent()
                testDispatcher.scheduler.advanceUntilIdle()
                val finalState = awaitItem()
                assertEquals(RideStatus.FINISHED, finalState.rideStatus)
                assertEquals(
                    EXPECTED_FARE_STRING_FINAL_NO_LUGGAGE,
                    finalState.totalFare.replace(',', '.')
                )
            }
        }

    @Test
    fun `GIVEN short ride (Route2) WHEN 1 luggage added and ride starts THEN totalFare is 5,60`() =
        runTest {
            val fakeShortRideFlow = flowOf(
                LocationPoint(40.416775, -3.703790, 1001L),
                LocationPoint(40.417775, -3.703790, 10000L),
                LocationPoint(40.418775, -3.703790, 20000L),
                LocationPoint(40.419775, -3.703790, 30000L),
                LocationPoint(40.420775, -3.703790, 40000L),
                LocationPoint(40.421775, -3.703790, 50000L)
            )

            coEvery {
                repository.getRideUpdates(
                    TEST_ROUTE_2,
                    TEST_EXECUTION_CONFIG
                )
            } returns fakeShortRideFlow
            viewModel = TaximeterViewModel(repository, mockStrategiesSet)
            viewModel.setTestRoute(TEST_ROUTE_2)
            viewModel.uiState.test {
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
                viewModel.onSupplementChange(TEST_SUPPLEMENT_ID, 1)
                runCurrent()
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
                viewModel.onStartStopClick()
                runCurrent()
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
                testDispatcher.scheduler.advanceUntilIdle()
                awaitItem()
                awaitItem()
                awaitItem()
                awaitItem()
                awaitItem()
                awaitItem()
                viewModel.onStartStopClick()
                runCurrent()
                testDispatcher.scheduler.advanceUntilIdle()
                val finalState = awaitItem()
                assertEquals(RideStatus.FINISHED, finalState.rideStatus)
                assertEquals(EXPECTED_DISTANCE_STRING_ROUTE_2, finalState.traveledDistance)
                assertEquals(EXPECTED_TIME_STRING_ROUTE_2, finalState.elapsedTime)
                assertEquals(
                    EXPECTED_FARE_STRING_ROUTE_2_FINAL,
                    finalState.totalFare.replace(',', '.')
                )
            }
        }

    companion object {
        private const val TEST_DISTANCE_FORMAT = "%.1f km"
        private const val TEST_DISTANCE_KM_ROUTE_2 = 0.555
        private const val TEST_TIME_FORMAT = "%02d:%02d:%02d"
        private const val TEST_DURATION_SECONDS_ROUTE_2 = 48L

        private const val TEST_LUGGAGE_COST = 5.0
        private const val TEST_PRICE_PER_KM = 0.2
        private const val TEST_PRICE_PER_SECOND = 0.01
        private const val TEST_CURRENCY_FORMAT = "%.2f €"

        private const val EXPECTED_FARE_DOUBLE_ROUTE_2_WITH_LUGGAGE =
            (TEST_DISTANCE_KM_ROUTE_2 * TEST_PRICE_PER_KM) +
                    (TEST_DURATION_SECONDS_ROUTE_2 * TEST_PRICE_PER_SECOND) +
                    TEST_LUGGAGE_COST
        private val EXPECTED_DISTANCE_STRING_ROUTE_2 =
            String.format(Locale.US, TEST_DISTANCE_FORMAT, TEST_DISTANCE_KM_ROUTE_2)
        private val EXPECTED_TIME_STRING_ROUTE_2 = String.format(
            Locale.US,
            TEST_TIME_FORMAT,
            0,
            0,
            TEST_DURATION_SECONDS_ROUTE_2
        )
        private val EXPECTED_FARE_STRING_ROUTE_2_FINAL = String.format(
            Locale.US,
            TEST_CURRENCY_FORMAT,
            EXPECTED_FARE_DOUBLE_ROUTE_2_WITH_LUGGAGE
        )

        private const val TEST_SUPPLEMENT_ID = "luggage"
        private const val TEST_DISTANCE_KM = 11.0
        private const val TEST_DURATION_SECONDS = 1800
        private const val EXPECTED_FINAL_FARE_DOUBLE = (TEST_DISTANCE_KM * TEST_PRICE_PER_KM) +
                (TEST_DURATION_SECONDS * TEST_PRICE_PER_SECOND) +
                TEST_LUGGAGE_COST

        private val EXPECTED_DISTANCE_STRING = String.format(Locale.US, "%.1f km", TEST_DISTANCE_KM)
        private val EXPECTED_TIME_STRING = String.format(Locale.US, "%02d:%02d:%02d", 0, 30, 0)
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

        private const val EXPECTED_FINAL_FARE_DOUBLE_NO_LUGGAGE =
            (TEST_DISTANCE_KM * TEST_PRICE_PER_KM) +
                    (TEST_DURATION_SECONDS * TEST_PRICE_PER_SECOND)
        private val EXPECTED_FARE_STRING_FINAL_NO_LUGGAGE = String.format(
            Locale.US,
            TEST_CURRENCY_FORMAT,
            EXPECTED_FINAL_FARE_DOUBLE_NO_LUGGAGE
        )

        private val TEST_ROUTE = RouteItem.Route1
        private val TEST_EXECUTION_CONFIG = ExecutionConfiguration.Fast
        private val TEST_ROUTE_2 = RouteItem.Route2
    }
}