package com.example.kuladig_app.utils

import com.google.android.gms.maps.model.LatLng

/**
 * Utility-Klasse für die Glättung von Polylines mit kubischen Bézier-Splines.
 * 
 * Diese Klasse berechnet automatisch Kontrollpunkte für kubische Bézier-Kurven
 * basierend auf benachbarten Punkten, um natürliche, glatte Kurven zu erzeugen.
 */
object BezierSplineUtil {
    
    /**
     * Glättet eine Liste von LatLng-Punkten mit kubischen Bézier-Splines.
     * 
     * @param points Die ursprünglichen Punkte der Polyline
     * @param segmentsPerCurve Anzahl der interpolierten Punkte pro Kurvensegment (Standard: 10)
     * @return Liste von geglätteten LatLng-Punkten
     */
    fun smoothPolyline(
        points: List<LatLng>,
        segmentsPerCurve: Int = 10
    ): List<LatLng> {
        // Wenn zu wenige Punkte, gibt die ursprüngliche Liste zurück
        if (points.size < 2) {
            return points
        }
        
        // Wenn nur 2 Punkte, keine Glättung möglich
        if (points.size == 2) {
            return points
        }
        
        val smoothedPoints = mutableListOf<LatLng>()
        
        // Füge den ersten Punkt hinzu (bleibt unverändert)
        smoothedPoints.add(points.first())
        
        // Glätte jeden Segment zwischen benachbarten Punkten
        for (i in 0 until points.size - 1) {
            val p0 = if (i > 0) points[i - 1] else points[i]
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = if (i < points.size - 2) points[i + 2] else points[i + 1]
            
            // Berechne Kontrollpunkte für kubische Bézier-Kurve
            val controlPoints = calculateControlPoints(p0, p1, p2, p3)
            
            // Interpoliere Punkte zwischen p1 und p2
            val interpolatedPoints = interpolateCubicBezier(
                p1,
                controlPoints.first,
                controlPoints.second,
                p2,
                segmentsPerCurve
            )
            
            // Füge interpolierte Punkte hinzu (ohne den ersten, da er bereits p1 ist)
            smoothedPoints.addAll(interpolatedPoints.drop(1))
        }
        
        return smoothedPoints
    }
    
    /**
     * Berechnet Kontrollpunkte für eine kubische Bézier-Kurve basierend auf vier Punkten.
     * Verwendet eine Catmull-Rom ähnliche Methode für natürliche Kurven.
     * 
     * @param p0 Punkt vor dem Start
     * @param p1 Startpunkt
     * @param p2 Endpunkt
     * @param p3 Punkt nach dem Ende
     * @return Paar von Kontrollpunkten (cp1, cp2)
     */
    private fun calculateControlPoints(
        p0: LatLng,
        p1: LatLng,
        p2: LatLng,
        p3: LatLng
    ): Pair<LatLng, LatLng> {
        // Spannung-Parameter (0.5 = Catmull-Rom, niedrigere Werte = weniger Kurven)
        val tension = 0.5
        
        // Berechne Richtungsvektoren
        val d1 = distance(p0, p1)
        val d2 = distance(p1, p2)
        val d3 = distance(p2, p3)
        
        // Vermeide Division durch Null
        val totalDistance = d1 + d2 + d3
        if (totalDistance == 0.0) {
            // Wenn alle Punkte gleich sind, verwende keine Kontrollpunkte
            return Pair(p1, p2)
        }
        
        // Gewichtete Richtungen für natürliche Kurven
        val t1 = if (d1 + d2 > 0) d2 / (d1 + d2) else 0.5
        val t2 = if (d2 + d3 > 0) d2 / (d2 + d3) else 0.5
        
        // Richtungsvektor am Startpunkt
        val dir1Lat = if (d1 + d2 > 0) {
            (p2.latitude - p0.latitude) * t1 * tension
        } else {
            0.0
        }
        val dir1Lng = if (d1 + d2 > 0) {
            (p2.longitude - p0.longitude) * t1 * tension
        } else {
            0.0
        }
        
        // Richtungsvektor am Endpunkt
        val dir2Lat = if (d2 + d3 > 0) {
            (p3.latitude - p1.latitude) * t2 * tension
        } else {
            0.0
        }
        val dir2Lng = if (d2 + d3 > 0) {
            (p3.longitude - p1.longitude) * t2 * tension
        } else {
            0.0
        }
        
        // Kontrollpunkt 1 (nach p1)
        val cp1 = LatLng(
            p1.latitude + dir1Lat / 3.0,
            p1.longitude + dir1Lng / 3.0
        )
        
        // Kontrollpunkt 2 (vor p2)
        val cp2 = LatLng(
            p2.latitude - dir2Lat / 3.0,
            p2.longitude - dir2Lng / 3.0
        )
        
        return Pair(cp1, cp2)
    }
    
    /**
     * Interpoliert Punkte entlang einer kubischen Bézier-Kurve.
     * 
     * @param p0 Startpunkt
     * @param cp1 Erster Kontrollpunkt
     * @param cp2 Zweiter Kontrollpunkt
     * @param p3 Endpunkt
     * @param segments Anzahl der zu generierenden Segmente
     * @return Liste von interpolierten Punkten
     */
    private fun interpolateCubicBezier(
        p0: LatLng,
        cp1: LatLng,
        cp2: LatLng,
        p3: LatLng,
        segments: Int
    ): List<LatLng> {
        val points = mutableListOf<LatLng>()
        
        for (i in 0..segments) {
            val t = i.toDouble() / segments.toDouble()
            
            // Kubische Bézier-Formel: B(t) = (1-t)³P₀ + 3(1-t)²tP₁ + 3(1-t)t²P₂ + t³P₃
            val u = 1.0 - t
            val tt = t * t
            val uu = u * u
            val uuu = uu * u
            val ttt = tt * t
            
            val latitude = uuu * p0.latitude +
                    3 * uu * t * cp1.latitude +
                    3 * u * tt * cp2.latitude +
                    ttt * p3.latitude
            
            val longitude = uuu * p0.longitude +
                    3 * uu * t * cp1.longitude +
                    3 * u * tt * cp2.longitude +
                    ttt * p3.longitude
            
            points.add(LatLng(latitude, longitude))
        }
        
        return points
    }
    
    /**
     * Berechnet die Distanz zwischen zwei LatLng-Punkten in Metern (Haversine-Formel).
     * 
     * @param p1 Erster Punkt
     * @param p2 Zweiter Punkt
     * @return Distanz in Metern
     */
    private fun distance(p1: LatLng, p2: LatLng): Double {
        val earthRadius = 6371000.0 // Radius der Erde in Metern
        
        val lat1Rad = Math.toRadians(p1.latitude)
        val lat2Rad = Math.toRadians(p2.latitude)
        val deltaLatRad = Math.toRadians(p2.latitude - p1.latitude)
        val deltaLngRad = Math.toRadians(p2.longitude - p1.longitude)
        
        val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLngRad / 2) * Math.sin(deltaLngRad / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
}
