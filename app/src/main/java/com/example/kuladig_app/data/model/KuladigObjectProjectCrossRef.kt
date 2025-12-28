package com.example.kuladig_app.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "kuladig_object_project_cross_ref",
    primaryKeys = ["kuladigObjectId", "projectId"],
    indices = [Index(value = ["projectId"])]
)
data class KuladigObjectProjectCrossRef(
    val kuladigObjectId: String,
    val projectId: Int
)

