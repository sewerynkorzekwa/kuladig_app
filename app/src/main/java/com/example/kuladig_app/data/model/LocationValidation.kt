package com.example.kuladig_app.data.model

import android.location.Location

/**
 * Ergebnis der Location-Validierung
 */
data class ValidationResult(
    val isValid: Boolean,
    val reason: ValidationReason? = null,
    val signalQuality: SignalQuality,
    val metadata: LocationMetadata
)

/**
 * Grund für ungültige Position
 */
enum class ValidationReason {
    ACCURACY_TOO_LOW,           // Genauigkeit zu niedrig
    OUTLIER_DETECTED,            // Outlier erkannt (Spring)
    SPEED_TOO_HIGH,              // Geschwindigkeit unrealistisch
    BEARING_INCONSISTENT,        // Bearing inkonsistent
    LOCATION_TOO_OLD,            // Position zu alt
    DISTANCE_IMPLAUSIBLE         // Distanz zwischen Updates unrealistisch
}

/**
 * Signalqualitätsbewertung
 */
enum class SignalQuality {
    EXCELLENT,   // Sehr gute Signalqualität (< 5m Genauigkeit)
    GOOD,        // Gute Signalqualität (5-10m Genauigkeit)
    FAIR,        // Mittlere Signalqualität (10-20m Genauigkeit)
    POOR         // Schlechte Signalqualität (> 20m Genauigkeit)
}

/**
 * Erweiterte Metadaten zu einer Position
 */
data class LocationMetadata(
    val accuracy: Float,              // Genauigkeit in Metern
    val speed: Float,                 // Geschwindigkeit in m/s
    val bearing: Float?,              // Bewegungsrichtung in Grad
    val timestamp: Long,             // Zeitstempel
    val provider: String?,           // GPS-Provider (GPS, Network, Passive)
    val altitude: Double,             // Höhe
    val isFromMockProvider: Boolean = false  // Ob von Mock-Provider
)

/**
 * Berechnet Signalqualität basierend auf Genauigkeit
 */
fun calculateSignalQuality(accuracy: Float): SignalQuality {
    return when {
        accuracy < 5f -> SignalQuality.EXCELLENT
        accuracy < 10f -> SignalQuality.GOOD
        accuracy < 20f -> SignalQuality.FAIR
        else -> SignalQuality.POOR
    }
}

/**
 * Erstellt LocationMetadata aus Location-Objekt
 */
fun Location.toLocationMetadata(): LocationMetadata {
    return LocationMetadata(
        accuracy = accuracy,
        speed = speed,
        bearing = if (hasBearing()) bearing else null,
        timestamp = time,
        provider = provider,
        altitude = altitude,
        isFromMockProvider = isFromMockProvider
    )
}
