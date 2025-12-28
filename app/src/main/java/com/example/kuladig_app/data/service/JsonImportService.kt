package com.example.kuladig_app.data.service

import android.content.Context
import android.content.SharedPreferences
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.KuladigObjectProjectCrossRef
import com.example.kuladig_app.data.model.KuladigResponse
import com.example.kuladig_app.data.model.Project
import com.example.kuladig_app.data.repository.KuladigRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

class JsonImportService(
    private val context: Context,
    private val repository: KuladigRepository,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val PREF_KEY_IMPORTED = "json_imported"
        private const val JSON_FILE_NAME = "kuladig_response.json"
    }

    suspend fun importIfNeeded(): Boolean {
        return withContext(Dispatchers.IO) {
            val alreadyImported = sharedPreferences.getBoolean(PREF_KEY_IMPORTED, false)
            if (alreadyImported) {
                return@withContext false
            }

            try {
                val inputStream = context.assets.open(JSON_FILE_NAME)
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                val json = Json { ignoreUnknownKeys = true }
                val response = json.decodeFromString<KuladigResponse>(jsonString)

                importData(response)

                sharedPreferences.edit().putBoolean(PREF_KEY_IMPORTED, true).apply()
                
                // Lösche die JSON-Datei nach erfolgreichem Import
                deleteJsonFile()
                
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    private suspend fun importData(response: KuladigResponse) {
        val allProjects = mutableSetOf<Pair<Int, String>>()
        val crossRefs = mutableListOf<KuladigObjectProjectCrossRef>()

        // Sammle alle Projekte und erstelle Cross-References
        response.Ergebnis.forEach { obj ->
            obj.Projekte.forEach { projectArray ->
                if (projectArray.size >= 2) {
                    val projectId = projectArray[0].jsonPrimitive.int
                    val projectName = projectArray[1].jsonPrimitive.content
                    allProjects.add(Pair(projectId, projectName))
                    crossRefs.add(
                        KuladigObjectProjectCrossRef(
                            kuladigObjectId = obj.Id,
                            projectId = projectId
                        )
                    )
                }
            }
        }

        // Konvertiere zu Entities
        val kuladigObjects = response.Ergebnis.map { obj ->
            KuladigObject(
                id = obj.Id,
                objekttyp = obj.Objekttyp,
                name = obj.Name,
                beschreibung = obj.Beschreibung,
                thumbnailToken = obj.ThumbnailToken,
                longitude = obj.Punktkoordinate.coordinates[0],
                latitude = obj.Punktkoordinate.coordinates[1],
                zuletztGeaendert = obj.ZuletztGeaendert
            )
        }

        val projects = allProjects.map { (id, name) ->
            Project(projektId = id, projektName = name)
        }

        // Importiere in Datenbank
        repository.insertAllObjects(kuladigObjects)
        repository.insertAllProjects(projects)
        repository.insertAllCrossRefs(crossRefs)
    }

    private suspend fun deleteJsonFile() {
        // Hinweis: Assets können zur Laufzeit nicht gelöscht werden,
        // da sie zur Build-Zeit in die APK kompiliert werden.
        // Die Datei wurde bereits aus dem Quellcode-Verzeichnis gelöscht,
        // sodass sie beim nächsten Build nicht mehr enthalten sein wird.
        // Nach dem ersten Import wird die Datei daher nicht mehr benötigt.
    }
}

