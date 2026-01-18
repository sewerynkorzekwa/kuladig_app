package com.example.kuladig_app.data.service

import android.location.Location
import com.example.kuladig_app.data.model.LocationMetadata
import com.example.kuladig_app.data.model.SignalQuality
import com.example.kuladig_app.data.model.ValidationReason
import com.example.kuladig_app.data.model.ValidationResult
import com.example.kuladig_app.data.model.calculateSignalQuality
import com.example.kuladig_app.data.model.toLocationMetadata
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Service für intelligente Location-Validierung mit Multi-Kriterien-Filterung
 */
class LocationValidator {
    
    companion object {
        // Konfigurationsparameter
        private const val MAX_ACCURACY_NAVIGATION = 10f      // Meter für Navigation
        private const val MAX_ACCURACY_NORMAL = 20f          // Meter für normale Nutzung
        private const val MAX_ACCURACY_BATTERY = 50f         // Meter für Batterie-Sparmodus
        
        private const val MAX_OUTLIER_DEVIATION = 100f       // Meter - max. Abweichung von erwarteter Position
        private const val MAX_SPEED_KMH = 200f              // km/h - unrealistische Geschwindigkeit
        private const val MAX_LOCATION_AGE_MS = 5000L        // ms - max. Alter der Position
        private const val MAX_BEARING_CHANGE = 90f           // Grad - max. Bearing-Änderung pro Update
        private const val MAX_DISTANCE_PER_UPDATE = 500f    // Meter - max. Distanz zwischen Updates
    }
    
    /**
     * Genauigkeitsprofil je nach Kontext
     */
    enum class AccuracyProfile {
        NAVIGATION,    // Sehr hohe Genauigkeit für Navigation
        NORMAL,        // Moderate Genauigkeit für normale Nutzung
        BATTERY        // Niedrigere Genauigkeit für Batterie-Sparmodus
    }
    
    /**
     * Validiert eine Location mit Multi-Kriterien-Filterung
     * 
     * @param location Die zu validierende Location
     * @param previousLocation Die vorherige Location (optional)
     * @param accuracyProfile Das Genauigkeitsprofil (Navigation/Normal/Batterie)
     * @return ValidationResult mit Validierungsstatus und Metadaten
     */
    fun validateLocation(
        location: Location,
        previousLocation: Location? = null,
        accuracyProfile: AccuracyProfile = AccuracyProfile.NORMAL
    ): ValidationResult {
        val metadata = location.toLocationMetadata()
        val signalQuality = calculateSignalQuality(location.accuracy)
        
        // 1. Genauigkeitsprüfung
        val maxAccuracy = when (accuracyProfile) {
            AccuracyProfile.NAVIGATION -> MAX_ACCURACY_NAVIGATION
            AccuracyProfile.NORMAL -> MAX_ACCURACY_NORMAL
            AccuracyProfile.BATTERY -> MAX_ACCURACY_BATTERY
        }
        
        if (location.accuracy > maxAccuracy) {
            return ValidationResult(
                isValid = false,
                reason = ValidationReason.ACCURACY_TOO_LOW,
                signalQuality = signalQuality,
                metadata = metadata
            )
        }
        
        // 2. Zeitbasierte Filterung
        val locationAge = System.currentTimeMillis() - location.time
        if (locationAge > MAX_LOCATION_AGE_MS) {
            return ValidationResult(
                isValid = false,
                reason = ValidationReason.LOCATION_TOO_OLD,
                signalQuality = signalQuality,
                metadata = metadata
            )
        }
        
        // Wenn keine vorherige Location vorhanden, ist die Position gültig
        if (previousLocation == null) {
            return ValidationResult(
                isValid = true,
                signalQuality = signalQuality,
                metadata = metadata
            )
        }
        
        // 3. Geschwindigkeitsbasierte Filterung
        val speedKmh = location.speed * 3.6f // m/s zu km/h
        if (speedKmh > MAX_SPEED_KMH) {
            return ValidationResult(
                isValid = false,
                reason = ValidationReason.SPEED_TOO_HIGH,
                signalQuality = signalQuality,
                metadata = metadata
            )
        }
        
        // 4. Distanz-Validierung
        val distance = previousLocation.distanceTo(location)
        if (distance > MAX_DISTANCE_PER_UPDATE) {
            return ValidationResult(
                isValid = false,
                reason = ValidationReason.DISTANCE_IMPLAUSIBLE,
                signalQuality = signalQuality,
                metadata = metadata
            )
        }
        
        // 5. Outlier-Erkennung (Spring-Erkennung)
        if (isOutlier(location, previousLocation)) {
            return ValidationResult(
                isValid = false,
                reason = ValidationReason.OUTLIER_DETECTED,
                signalQuality = signalQuality,
                metadata = metadata
            )
        }
        
        // 6. Bearing-Konsistenz-Check
        if (isBearingInconsistent(location, previousLocation)) {
            return ValidationResult(
                isValid = false,
                reason = ValidationReason.BEARING_INCONSISTENT,
                signalQuality = signalQuality,
                metadata = metadata
            )
        }
        
        // Alle Validierungen bestanden
        return ValidationResult(
            isValid = true,
            signalQuality = signalQuality,
            metadata = metadata
        )
    }
    
    /**
     * Prüft, ob eine Location ein Outlier (Spring) ist
     * Berechnet erwartete Position basierend auf vorheriger Position, Geschwindigkeit und Bearing
     */
    private fun isOutlier(
        location: Location,
        previousLocation: Location
    ): Boolean {
        // Berechne Zeitdifferenz in Sekunden
        val timeDelta = (location.time - previousLocation.time) / 1000.0
        
        // Wenn Zeitdifferenz zu klein oder negativ, überspringe Outlier-Check
        if (timeDelta <= 0 || timeDelta > 10) {
            return false
        }
        
        // Berechne erwartete Position basierend auf Geschwindigkeit und Bearing
        val speed = previousLocation.speed // m/s
        val distance = speed * timeDelta // Meter
        
        // Wenn keine Geschwindigkeit oder Bearing vorhanden, überspringe
        if (distance < 1 || !previousLocation.hasBearing()) {
            return false
        }
        
        val bearing = previousLocation.bearing.toDouble()
        
        // Berechne erwartete Koordinaten (vereinfachte Berechnung)
        val earthRadius = 6371000.0 // Meter
        val lat1 = Math.toRadians(previousLocation.latitude)
        val lon1 = Math.toRadians(previousLocation.longitude)
        
        val lat2 = Math.asin(
            Math.sin(lat1) * Math.cos(distance / earthRadius) +
            Math.cos(lat1) * Math.sin(distance / earthRadius) * Math.cos(Math.toRadians(bearing))
        )
        
        val lon2 = lon1 + Math.atan2(
            Math.sin(Math.toRadians(bearing)) * Math.sin(distance / earthRadius) * Math.cos(lat1),
            Math.cos(distance / earthRadius) - Math.sin(lat1) * Math.sin(lat2)
        )
        
        val expectedLat = Math.toDegrees(lat2)
        val expectedLon = Math.toDegrees(lon2)
        
        // Erstelle erwartete Location
        val expectedLocation = Location(previousLocation.provider)
        expectedLocation.latitude = expectedLat
        expectedLocation.longitude = expectedLon
        
        // Berechne Abweichung von erwarteter Position
        val deviation = location.distanceTo(expectedLocation)
        
        // Berücksichtige Genauigkeit beider Positionen
        val maxDeviation = MAX_OUTLIER_DEVIATION + 
            max(location.accuracy, previousLocation.accuracy)
        
        return deviation > maxDeviation
    }
    
    /**
     * Prüft, ob das Bearing inkonsistent ist (zu große Änderung)
     */
    private fun isBearingInconsistent(
        location: Location,
        previousLocation: Location
    ): Boolean {
        // Wenn keine Bearings vorhanden, überspringe Check
        if (!location.hasBearing() || !previousLocation.hasBearing()) {
            return false
        }
        
        val currentBearing = location.bearing
        val previousBearing = previousLocation.bearing
        
        // Normalisiere Bearings auf 0-360
        fun normalizeBearing(bearing: Float): Float {
            var normalized = bearing % 360f
            if (normalized < 0) normalized += 360f
            return normalized
        }
        
        val normalizedCurrent = normalizeBearing(currentBearing)
        val normalizedPrevious = normalizeBearing(previousBearing)
        
        // Berechne minimale Änderung (berücksichtigt 360°-Übergang)
        val change1 = abs(normalizedCurrent - normalizedPrevious)
        val change2 = 360f - change1
        val minChange = min(change1, change2)
        
        // Wenn Änderung zu groß, ist Bearing inkonsistent
        return minChange > MAX_BEARING_CHANGE
    }
    
    /**
     * Berechnet Signalqualität basierend auf mehreren Faktoren
     */
    fun calculateSignalQuality(
        location: Location,
        previousLocations: List<Location> = emptyList()
    ): SignalQuality {
        val baseQuality = calculateSignalQuality(location.accuracy)
        
        // Wenn mehrere Positionen vorhanden, berücksichtige Konsistenz
        if (previousLocations.isNotEmpty()) {
            val recentLocations = previousLocations.takeLast(5)
            val avgAccuracy = recentLocations.map { it.accuracy }.average().toFloat()
            val avgQuality = calculateSignalQuality(avgAccuracy)
            
            // Verwende schlechtere Qualität (konservativer Ansatz)
            return if (avgQuality.ordinal > baseQuality.ordinal) {
                avgQuality
            } else {
                baseQuality
            }
        }
        
        return baseQuality
    }
}
