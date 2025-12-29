package com.example.kuladig_app.data.service

import com.example.kuladig_app.data.model.DirectionsResponse
import com.example.kuladig_app.data.model.Route
import com.example.kuladig_app.data.model.TravelMode
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType

class DirectionsService(private val apiKey: String) {
    private val baseUrl = "https://maps.googleapis.com/maps/api/"
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()
    
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
    
    private val api: DirectionsApi = retrofit.create(DirectionsApi::class.java)
    
    /**
     * Berechnet eine Route von einem Startpunkt zu einem Zielpunkt
     * @param origin Startposition als LatLng
     * @param destination Zielposition als LatLng
     * @param mode Transportmodus (WALKING oder DRIVING)
     * @return Route-Objekt oder null bei Fehler
     */
    suspend fun getRoute(
        origin: LatLng,
        destination: LatLng,
        mode: TravelMode
    ): Result<Route> = withContext(Dispatchers.IO) {
        try {
            val originStr = "${origin.latitude},${origin.longitude}"
            val destinationStr = "${destination.latitude},${destination.longitude}"
            val modeStr = when (mode) {
                TravelMode.WALKING -> "walking"
                TravelMode.DRIVING -> "driving"
            }
            
            val response = api.getDirections(originStr, destinationStr, modeStr, apiKey)
            
            if (response.status == "OK" && response.routes.isNotEmpty()) {
                Result.success(response.routes.first())
            } else {
                Result.failure(Exception("API Error: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Dekodiert eine encoded polyline zu einer Liste von LatLng-Punkten
     */
    fun decodePolyline(encodedPolyline: String): List<LatLng> {
        return PolyUtil.decode(encodedPolyline)
    }
}

