package com.taximeter.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.taximeter.data.database.AppDatabase
import com.taximeter.data.database.PriceConfigDao
import com.taximeter.data.database.toEntity
import com.taximeter.data.location.LocationProvider
import com.taximeter.data.network.ApiService
import com.taximeter.data.network.dto.PriceConfigDto
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@RunWith(AndroidJUnit4::class)
class TaximeterRepositoryImplTest {

    private lateinit var repository: TaximeterRepositoryImpl
    private lateinit var database: AppDatabase
    private lateinit var dao: PriceConfigDao
    private lateinit var mockWebServer: MockWebServer
    private lateinit var apiService: ApiService
    private val mockLocationProvider: LocationProvider = mockk(relaxed = true)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.priceConfigDao()

        mockWebServer = MockWebServer()
        mockWebServer.start()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        apiService = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ApiService::class.java)

        repository = TaximeterRepositoryImpl(apiService, dao, mockLocationProvider)
    }

    @After
    fun tearDown() {
        database.close()
        mockWebServer.shutdown()
    }

    @Test
    fun fetchPriceConfigIfNeeded_whenCacheIsEmpty_fetchesFromApiAndSavesToCache() = runTest {
        val mockJsonResponse = """
            {
                "$KEY_PRICE_PER_KM": $API_PRICE_PER_KM,
                "$KEY_PRICE_PER_SECOND": $API_PRICE_PER_SECOND
            }
        """.trimIndent()
        mockWebServer.enqueue(MockResponse().setBody(mockJsonResponse))

        assertEquals(0, dao.getConfigCount())

        repository.fetchPriceConfigIfNeeded()

        assertEquals(1, mockWebServer.requestCount)
        val configFromDb = repository.getPriceConfig().first()
        assertEquals(API_PRICE_PER_KM, configFromDb?.pricePerKm)
    }

    @Test
    fun fetchPriceConfigIfNeeded_whenCacheIsFull_doesNotFetchFromApi() = runTest {
        dao.insertConfig(PriceConfigDto(CACHE_PRICE_PER_KM, CACHE_PRICE_PER_SECOND).toEntity())
        assertEquals(1, dao.getConfigCount())

        repository.fetchPriceConfigIfNeeded()

        assertEquals(0, mockWebServer.requestCount)
        val configFromDb = repository.getPriceConfig().first()
        assertEquals(CACHE_PRICE_PER_KM, configFromDb?.pricePerKm)
    }

    @Test
    fun fetchPriceConfigIfNeeded_whenApiReturns404_doesNotCrashAndCacheRemainsEmpty() = runTest {
        mockWebServer.enqueue(MockResponse().setResponseCode(404))
        assertEquals(0, dao.getConfigCount())
        repository.fetchPriceConfigIfNeeded()
        assertEquals(1, mockWebServer.requestCount)
        assertEquals(0, dao.getConfigCount())
    }

    @Test
    fun fetchPriceConfigIfNeeded_whenApiReturnsMalformedJson_doesNotCrashAndCacheRemainsEmpty() = runTest {
        val malformedJson = "{ \"$KEY_PRICE_PER_KM\": 0.2, "
        mockWebServer.enqueue(MockResponse().setBody(malformedJson))
        assertEquals(0, dao.getConfigCount())
        repository.fetchPriceConfigIfNeeded()
        assertEquals(1, mockWebServer.requestCount)
        assertEquals(0, dao.getConfigCount())
    }

    @Test
    fun fetchPriceConfigIfNeeded_whenApiTimesOut_doesNotCrash() = runTest {
        mockWebServer.enqueue(
            MockResponse().setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START)
        )
        assertEquals(0, dao.getConfigCount())
        repository.fetchPriceConfigIfNeeded()
        assertEquals(1, mockWebServer.requestCount)
        assertEquals(0, dao.getConfigCount())
    }

    companion object {
        private const val KEY_PRICE_PER_KM = "price_per_km"
        private const val KEY_PRICE_PER_SECOND = "price_per_second"

        private const val API_PRICE_PER_KM = 0.2
        private const val API_PRICE_PER_SECOND = 0.01

        private const val CACHE_PRICE_PER_KM = 0.5
        private const val CACHE_PRICE_PER_SECOND = 0.5
    }
}