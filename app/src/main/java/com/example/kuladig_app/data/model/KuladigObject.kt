package com.example.kuladig_app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kuladig_objects")
data class KuladigObject(
    @PrimaryKey
    val id: String,
    val objekttyp: String,
    val name: String,
    val beschreibung: String,
    val thumbnailToken: String?,
    val longitude: Double,
    val latitude: Double,
    val zuletztGeaendert: String
)

