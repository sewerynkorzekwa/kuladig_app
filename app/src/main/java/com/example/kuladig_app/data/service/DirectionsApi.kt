package com.example.kuladig_app.data.service

import com.example.kuladig_app.data.model.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsApi {
    @GET("directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String,
        @Query("key") apiKey: String
    ): DirectionsResponse

    @GET("directions/json")
    suspend fun getDirectionsWithWaypoints(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("waypoints") waypoints: String,
        @Query("mode") mode: String,
        @Query("key") apiKey: String
    ): DirectionsResponse
}

