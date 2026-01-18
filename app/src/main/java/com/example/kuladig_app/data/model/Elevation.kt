package com.example.kuladig_app.data.model

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable

/**
 * Response von der Google Elevation API
 */
@Serializable
data class ElevationResponse(
    val results: List<ElevationResult>,
    val status: String
)

/**
 * Einzelnes Höhenresultat von der Elevation API
 */
@Serializable
data class ElevationResult(
    val elevation: Double, // Höhe in Metern
    val location: ElevationLocation,
    val resolution: Double? = null // Auflösung in Metern
)

/**
 * Location-Koordinate in Elevation API Response
 */
@Serializable
data class ElevationLocation(
    val lat: Double,
    val lng: Double
)

/**
 * Punkt mit Koordinate und Höhe
 */
data class ElevationPoint(
    val location: LatLng,
    val elevation: Double, // Höhe in Metern
    val distance: Double = 0.0 // Distanz entlang der Route in Metern
)

/**
 * Verarbeitetes Höhenprofil mit Statistiken
 */
data class ElevationProfile(
    val points: List<ElevationPoint>,
    val minElevation: Double,
    val maxElevation: Double,
    val totalAscent: Double, // Gesamtanstieg in Metern
    val totalDescent: Double, // Gesamtabstieg in Metern
    val totalDistance: Double // Gesamtdistanz in Metern
) {
    /**
     * Berechnet die Höhendifferenz (max - min)
     */
    val elevationRange: Double
        get() = maxElevation - minElevation
}
