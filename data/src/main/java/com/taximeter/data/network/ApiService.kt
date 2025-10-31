package com.taximeter.data.network

import com.taximeter.data.network.dto.PriceConfigDto
import retrofit2.http.GET

interface ApiService {

    @GET("alhimacabify/a535399cd77a94f1b67e50d2d41258e1/raw/5bdccd0a0f060d23325d613fb513f942ad9315b0/priceconfig.json")
    suspend fun getPriceConfig(): PriceConfigDto
}