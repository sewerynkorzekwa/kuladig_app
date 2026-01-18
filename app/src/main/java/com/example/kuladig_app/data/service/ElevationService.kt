package com.example.kuladig_app.data.service

import com.example.kuladig_app.data.model.ElevationPoint
import com.example.kuladig_app.data.model.ElevationProfile
import com.example.kuladig_app.data.model.ElevationResponse
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import kotlin.math.*

class ElevationService(private val apiKey: String) {
    private val baseUrl = "https://maps.googleapis.com/maps/api/"
    private val maxPointsPerRequest = 512 // Google Elevation API Limit
    
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
    
    private val api: ElevationApi = retrofit.create(ElevationApi::class.java)
    
    /**
     * Berechnet die Distanz zwischen zwei Koordinaten in Metern (Haversine-Formel)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // Radius der Erde in Metern
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }
    
    /**
     * Berechnet kumulative Distanzen entlang einer Route
     */
    private fun calculateDistances(points: List<LatLng>): List<Double> {
        val distances = mutableListOf<Double>()
        var cumulativeDistance = 0.0
        
        distances.add(0.0) // Startpunkt hat Distanz 0
        
        for (i in 1 until points.size) {
            val distance = calculateDistance(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude
            )
            cumulativeDistance += distance
            distances.add(cumulativeDistance)
        }
        
        return distances
    }
    
    /**
     * Wählt repräsentative Punkte aus einer Liste aus (Sampling)
     * Versucht wichtige Punkte (Minima, Maxima, Wendepunkte) zu erhalten
     */
    private fun samplePoints(points: List<LatLng>, maxPoints: Int): List<LatLng> {
        if (points.size <= maxPoints) {
            return points
        }
        
        val sampled = mutableListOf<LatLng>()
        val step = points.size / maxPoints.toDouble()
        
        // Immer Start- und Endpunkt einbeziehen
        sampled.add(points.first())
        
        // Gleichmäßige Stichproben mit leichter Variation
        for (i in 1 until maxPoints - 1) {
            val index = (i * step).toInt().coerceIn(1, points.size - 2)
            sampled.add(points[index])
        }
        
        sampled.add(points.last())
        
        return sampled
    }
    
    /**
     * Ruft Höhendaten für eine Liste von Koordinaten ab
     * Teilt automatisch in mehrere Requests auf, wenn mehr als maxPointsPerRequest Punkte vorhanden sind
     */
    suspend fun getElevationForLocations(
        locations: List<LatLng>
    ): Result<List<ElevationPoint>> = withContext(Dispatchers.IO) {
        try {
            if (locations.isEmpty()) {
                return@withContext Result.success(emptyList())
            }
            
            val allResults = mutableListOf<ElevationPoint>()
            
            // Wenn mehr als maxPointsPerRequest Punkte, in Chunks aufteilen
            if (locations.size > maxPointsPerRequest) {
                val chunks = locations.chunked(maxPointsPerRequest)
                
                for (chunk in chunks) {
                    val locationsStr = chunk.joinToString("|") { "${it.latitude},${it.longitude}" }
                    val response = api.getElevationForLocations(locationsStr, apiKey)
                    
                    if (response.status == "OK") {
                        val points = response.results.mapIndexed { index, result ->
                            ElevationPoint(
                                location = LatLng(result.location.lat, result.location.lng),
                                elevation = result.elevation
                            )
                        }
                        allResults.addAll(points)
                    } else {
                        return@withContext Result.failure(Exception("Elevation API Error: ${response.status}"))
                    }
                }
            } else {
                val locationsStr = locations.joinToString("|") { "${it.latitude},${it.longitude}" }
                val response = api.getElevationForLocations(locationsStr, apiKey)
                
                if (response.status == "OK") {
                    val points = response.results.mapIndexed { index, result ->
                        ElevationPoint(
                            location = LatLng(result.location.lat, result.location.lng),
                            elevation = result.elevation
                        )
                    }
                    allResults.addAll(points)
                } else {
                    return@withContext Result.failure(Exception("Elevation API Error: ${response.status}"))
                }
            }
            
            Result.success(allResults)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Ruft Höhendaten entlang einer Polyline ab
     * @param encodedPolyline Encoded polyline string
     * @param samples Anzahl der gewünschten Stichproben (optional, Standard: alle Punkte bis max 512)
     * @return Liste von ElevationPoints mit Höhe und Distanz
     */
    suspend fun getElevationAlongPath(
        encodedPolyline: String,
        samples: Int? = null
    ): Result<ElevationProfile> = withContext(Dispatchers.IO) {
        try {
            // Dekodiere Polyline
            val allPoints = PolyUtil.decode(encodedPolyline)
            
            if (allPoints.isEmpty()) {
                return@withContext Result.failure(Exception("Empty polyline"))
            }
            
            // Wähle repräsentative Punkte falls nötig
            val targetSamples = samples ?: min(allPoints.size, maxPointsPerRequest)
            val sampledPoints = samplePoints(allPoints, targetSamples)
            
            // Hole Höhendaten
            val elevationResult = getElevationForLocations(sampledPoints)
            
            elevationResult.fold(
                onSuccess = { elevationPoints ->
                    // Berechne Distanzen entlang der Route
                    val distances = calculateDistances(sampledPoints)
                    
                    // Kombiniere Höhen mit Distanzen
                    val pointsWithDistance = elevationPoints.mapIndexed { index, point ->
                        point.copy(distance = distances[index])
                    }
                    
                    // Berechne Statistiken
                    val elevations = pointsWithDistance.map { it.elevation }
                    val minElevation = elevations.minOrNull() ?: 0.0
                    val maxElevation = elevations.maxOrNull() ?: 0.0
                    
                    // Berechne Gesamtanstieg und -abstieg
                    var totalAscent = 0.0
                    var totalDescent = 0.0
                    
                    for (i in 1 until pointsWithDistance.size) {
                        val elevationDiff = pointsWithDistance[i].elevation - pointsWithDistance[i - 1].elevation
                        if (elevationDiff > 0) {
                            totalAscent += elevationDiff
                        } else {
                            totalDescent += abs(elevationDiff)
                        }
                    }
                    
                    val totalDistance = distances.lastOrNull() ?: 0.0
                    
                    val profile = ElevationProfile(
                        points = pointsWithDistance,
                        minElevation = minElevation,
                        maxElevation = maxElevation,
                        totalAscent = totalAscent,
                        totalDescent = totalDescent,
                        totalDistance = totalDistance
                    )
                    
                    Result.success(profile)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Ruft Höhendaten für eine Liste von LatLng-Punkten entlang einer Route ab
     * @param points Liste von LatLng-Punkten (z.B. aus einer dekodierten Polyline)
     * @return ElevationProfile mit Höhendaten und Statistiken
     */
    suspend fun getElevationForRoutePoints(
        points: List<LatLng>
    ): Result<ElevationProfile> = withContext(Dispatchers.IO) {
        try {
            if (points.isEmpty()) {
                return@withContext Result.failure(Exception("Empty points list"))
            }
            
            // Wähle repräsentative Punkte falls nötig
            val sampledPoints = samplePoints(points, maxPointsPerRequest)
            
            // Hole Höhendaten
            val elevationResult = getElevationForLocations(sampledPoints)
            
            elevationResult.fold(
                onSuccess = { elevationPoints ->
                    // Berechne Distanzen entlang der Route
                    val distances = calculateDistances(sampledPoints)
                    
                    // Kombiniere Höhen mit Distanzen
                    val pointsWithDistance = elevationPoints.mapIndexed { index, point ->
                        point.copy(distance = distances[index])
                    }
                    
                    // Berechne Statistiken
                    val elevations = pointsWithDistance.map { it.elevation }
                    val minElevation = elevations.minOrNull() ?: 0.0
                    val maxElevation = elevations.maxOrNull() ?: 0.0
                    
                    // Berechne Gesamtanstieg und -abstieg
                    var totalAscent = 0.0
                    var totalDescent = 0.0
                    
                    for (i in 1 until pointsWithDistance.size) {
                        val elevationDiff = pointsWithDistance[i].elevation - pointsWithDistance[i - 1].elevation
                        if (elevationDiff > 0) {
                            totalAscent += elevationDiff
                        } else {
                            totalDescent += abs(elevationDiff)
                        }
                    }
                    
                    val totalDistance = distances.lastOrNull() ?: 0.0
                    
                    val profile = ElevationProfile(
                        points = pointsWithDistance,
                        minElevation = minElevation,
                        maxElevation = maxElevation,
                        totalAscent = totalAscent,
                        totalDescent = totalDescent,
                        totalDistance = totalDistance
                    )
                    
                    Result.success(profile)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
