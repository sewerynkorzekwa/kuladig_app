package com.example.kuladig_app.data.service

import com.example.kuladig_app.data.model.ElevationResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ElevationApi {
    /**
     * Ruft Höhendaten für eine Liste von Koordinaten ab
     * @param locations Komma-separierte Liste von Koordinaten im Format "lat,lng|lat,lng|..."
     * @param apiKey Google Maps API Key
     * @return ElevationResponse mit Höhendaten für alle angegebenen Koordinaten
     */
    @GET("elevation/json")
    suspend fun getElevationForLocations(
        @Query("locations") locations: String,
        @Query("key") apiKey: String
    ): ElevationResponse

    /**
     * Ruft Höhendaten entlang einer Polyline ab
     * @param path Encoded polyline string oder Koordinaten im Format "lat,lng|lat,lng|..."
     * @param samples Anzahl der Stichproben entlang des Pfads (optional, Standard: Anzahl der Punkte)
     * @param apiKey Google Maps API Key
     * @return ElevationResponse mit Höhendaten entlang des Pfads
     */
    @GET("elevation/json")
    suspend fun getElevationAlongPath(
        @Query("path") path: String,
        @Query("samples") samples: Int? = null,
        @Query("key") apiKey: String
    ): ElevationResponse
}
