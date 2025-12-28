package com.example.kuladig_app.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class KuladigObjectWithProjects(
    @Embedded val kuladigObject: KuladigObject,
    @Relation(
        parentColumn = "id",
        entityColumn = "projektId",
        associateBy = androidx.room.Junction(
            value = KuladigObjectProjectCrossRef::class,
            parentColumn = "kuladigObjectId",
            entityColumn = "projectId"
        )
    )
    val projects: List<Project>
)

