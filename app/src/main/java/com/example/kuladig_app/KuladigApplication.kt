package com.example.kuladig_app

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.example.kuladig_app.data.dao.KuladigDao
import com.example.kuladig_app.data.dao.KuladigObjectProjectCrossRefDao
import com.example.kuladig_app.data.dao.ProjectDao
import com.example.kuladig_app.data.database.KuladigDatabase
import com.example.kuladig_app.data.repository.KuladigRepository
import com.example.kuladig_app.data.service.JsonImportService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KuladigApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: KuladigDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            KuladigDatabase::class.java,
            "kuladig_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository: KuladigRepository by lazy {
        KuladigRepository(
            kuladigDao = database.kuladigDao(),
            projectDao = database.projectDao(),
            crossRefDao = database.kuladigObjectProjectCrossRefDao()
        )
    }

    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences("kuladig_prefs", Context.MODE_PRIVATE)
    }

    val jsonImportService: JsonImportService by lazy {
        JsonImportService(
            context = applicationContext,
            repository = repository,
            sharedPreferences = sharedPreferences
        )
    }

    override fun onCreate() {
        super.onCreate()
        
        // Importiere Daten beim ersten Start
        applicationScope.launch {
            jsonImportService.importIfNeeded()
        }
    }
}

