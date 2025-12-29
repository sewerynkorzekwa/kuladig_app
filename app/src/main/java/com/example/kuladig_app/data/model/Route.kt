package com.example.kuladig_app.data.model

import kotlinx.serialization.Serializable

enum class TravelMode {
    WALKING,
    DRIVING
}

@Serializable
data class DirectionsResponse(
    val routes: List<Route>,
    val status: String
)

@Serializable
data class Route(
    val legs: List<Leg>,
    val overview_polyline: OverviewPolyline
)

@Serializable
data class Leg(
    val distance: Distance,
    val duration: Duration,
    val start_location: Location,
    val end_location: Location,
    val steps: List<Step>
)

@Serializable
data class Step(
    val distance: Distance,
    val duration: Duration,
    val start_location: Location,
    val end_location: Location,
    val polyline: OverviewPolyline,
    val html_instructions: String
)

@Serializable
data class Distance(
    val text: String,
    val value: Int // in Metern
)

@Serializable
data class Duration(
    val text: String,
    val value: Int // in Sekunden
)

@Serializable
data class Location(
    val lat: Double,
    val lng: Double
)

@Serializable
data class OverviewPolyline(
    val points: String // encoded polyline string
)

