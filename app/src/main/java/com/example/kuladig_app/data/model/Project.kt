package com.example.kuladig_app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey
    val projektId: Int,
    val projektName: String
)

