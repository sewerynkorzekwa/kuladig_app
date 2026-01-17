package com.example.kuladig_app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "tours")
data class Tour(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val travelMode: TravelMode
)

@Entity(
    tableName = "tour_stops",
    foreignKeys = [
        ForeignKey(
            entity = Tour::class,
            parentColumns = ["id"],
            childColumns = ["tourId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = KuladigObject::class,
            parentColumns = ["id"],
            childColumns = ["kuladigObjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TourStop(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tourId: Long,
    val kuladigObjectId: String,
    val stopOrder: Int,
    val notes: String? = null
)
