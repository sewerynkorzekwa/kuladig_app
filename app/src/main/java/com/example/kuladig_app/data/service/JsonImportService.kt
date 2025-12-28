package com.example.kuladig_app.data.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
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
        private const val TAG = "JsonImportService"
        private const val PREF_KEY_IMPORTED = "json_imported"
        private const val JSON_FILE_NAME = "kuladig_response.json"
    }

    suspend fun importIfNeeded(): Boolean {
        return withContext(Dispatchers.IO) {
            val alreadyImported = sharedPreferences.getBoolean(PREF_KEY_IMPORTED, false)
            
            // Prüfe sowohl den Flag als auch ob die Datenbank tatsächlich Daten enthält
            if (alreadyImported) {
                val isDatabaseEmpty = repository.isDatabaseEmpty()
                if (!isDatabaseEmpty) {
                    // Datenbank enthält Daten, Import nicht nötig
                    Log.d(TAG, "Import bereits durchgeführt und Datenbank enthält Daten")
                    return@withContext false
                }
                // Flag ist gesetzt, aber Datenbank ist leer - Import erneut durchführen
                Log.w(TAG, "Import-Flag ist gesetzt, aber Datenbank ist leer. Führe Import erneut durch.")
            }

            try {
                Log.d(TAG, "Starte Import der JSON-Daten...")
                val inputStream = context.assets.open(JSON_FILE_NAME)
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                inputStream.close()

                val json = Json { ignoreUnknownKeys = true }
                val response = json.decodeFromString<KuladigResponse>(jsonString)

                Log.d(TAG, "JSON-Datei erfolgreich gelesen. Anzahl Objekte: ${response.Ergebnis.size}")
                importData(response)

                // Flag nur bei erfolgreichem Import setzen
                sharedPreferences.edit().putBoolean(PREF_KEY_IMPORTED, true).apply()
                Log.d(TAG, "Import erfolgreich abgeschlossen. Daten wurden in die Datenbank eingefügt.")
                
                // Lösche die JSON-Datei nach erfolgreichem Import
                deleteJsonFile()
                
                true
            } catch (e: java.io.FileNotFoundException) {
                Log.e(TAG, "JSON-Datei '$JSON_FILE_NAME' nicht in Assets gefunden", e)
                // Flag wird bei Fehler nicht gesetzt, damit Import beim nächsten Start erneut versucht wird
                false
            } catch (e: kotlinx.serialization.SerializationException) {
                Log.e(TAG, "Fehler beim Deserialisieren der JSON-Datei", e)
                // Flag wird bei Fehler nicht gesetzt, damit Import beim nächsten Start erneut versucht wird
                false
            } catch (e: Exception) {
                Log.e(TAG, "Unerwarteter Fehler beim Import", e)
                // Flag wird bei Fehler nicht gesetzt, damit Import beim nächsten Start erneut versucht wird
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
        Log.d(TAG, "Füge ${kuladigObjects.size} Objekte, ${projects.size} Projekte und ${crossRefs.size} Cross-References in die Datenbank ein...")
        repository.insertAllObjects(kuladigObjects)
        repository.insertAllProjects(projects)
        repository.insertAllCrossRefs(crossRefs)
        Log.d(TAG, "Daten erfolgreich in die Datenbank eingefügt")
    }

    private suspend fun deleteJsonFile() {
        // Hinweis: Assets können zur Laufzeit nicht gelöscht werden,
        // da sie zur Build-Zeit in die APK kompiliert werden.
        // Die Datei wurde bereits aus dem Quellcode-Verzeichnis gelöscht,
        // sodass sie beim nächsten Build nicht mehr enthalten sein wird.
        // Nach dem ersten Import wird die Datei daher nicht mehr benötigt.
    }
}

