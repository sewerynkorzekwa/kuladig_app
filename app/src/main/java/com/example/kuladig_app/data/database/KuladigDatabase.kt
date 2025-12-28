package com.example.kuladig_app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.kuladig_app.data.dao.KuladigDao
import com.example.kuladig_app.data.dao.KuladigObjectProjectCrossRefDao
import com.example.kuladig_app.data.dao.ProjectDao
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.KuladigObjectProjectCrossRef
import com.example.kuladig_app.data.model.Project

@Database(
    entities = [
        KuladigObject::class,
        Project::class,
        KuladigObjectProjectCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class KuladigDatabase : RoomDatabase() {
    abstract fun kuladigDao(): KuladigDao
    abstract fun projectDao(): ProjectDao
    abstract fun kuladigObjectProjectCrossRefDao(): KuladigObjectProjectCrossRefDao
}

