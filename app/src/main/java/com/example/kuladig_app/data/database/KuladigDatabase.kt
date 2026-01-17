package com.example.kuladig_app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.kuladig_app.data.dao.KuladigDao
import com.example.kuladig_app.data.dao.KuladigObjectProjectCrossRefDao
import com.example.kuladig_app.data.dao.ProjectDao
import com.example.kuladig_app.data.dao.TourDao
import com.example.kuladig_app.data.dao.TourStopDao
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.KuladigObjectProjectCrossRef
import com.example.kuladig_app.data.model.Project
import com.example.kuladig_app.data.model.Tour
import com.example.kuladig_app.data.model.TourStop

@Database(
    entities = [
        KuladigObject::class,
        Project::class,
        KuladigObjectProjectCrossRef::class,
        Tour::class,
        TourStop::class
    ],
    version = 3,
    exportSchema = false
)
abstract class KuladigDatabase : RoomDatabase() {
    abstract fun kuladigDao(): KuladigDao
    abstract fun projectDao(): ProjectDao
    abstract fun kuladigObjectProjectCrossRefDao(): KuladigObjectProjectCrossRefDao
    abstract fun tourDao(): TourDao
    abstract fun tourStopDao(): TourStopDao
}

