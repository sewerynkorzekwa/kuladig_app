package com.example.kuladig_app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class KuladigResponse(
    val Anzahl: Int,
    val Gesamtanzahl: Int,
    val Seite: Int,
    val AnzahlSeiten: Int,
    val Ergebnis: List<KuladigObjectJson>
)

@Serializable
data class KuladigObjectJson(
    val Objekttyp: String,
    val Id: String,
    val Name: String,
    val Beschreibung: String,
    val Projekte: List<List<kotlinx.serialization.json.JsonElement>>,
    val ThumbnailToken: String?,
    val Punktkoordinate: PunktkoordinateJson,
    val ZuletztGeaendert: String
)

@Serializable
data class PunktkoordinateJson(
    val type: String,
    val coordinates: List<Double>
)

