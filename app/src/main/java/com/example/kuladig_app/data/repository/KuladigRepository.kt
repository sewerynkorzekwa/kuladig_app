package com.example.kuladig_app.data.repository

import com.example.kuladig_app.data.dao.KuladigDao
import com.example.kuladig_app.data.dao.KuladigObjectProjectCrossRefDao
import com.example.kuladig_app.data.dao.ProjectDao
import com.example.kuladig_app.data.dao.TourDao
import com.example.kuladig_app.data.dao.TourStopDao
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.KuladigObjectWithProjects
import com.example.kuladig_app.data.model.Project
import com.example.kuladig_app.data.model.Tour
import com.example.kuladig_app.data.model.TourStop

class KuladigRepository(
    private val kuladigDao: KuladigDao,
    private val projectDao: ProjectDao,
    private val crossRefDao: KuladigObjectProjectCrossRefDao,
    private val tourDao: TourDao,
    private val tourStopDao: TourStopDao
) {
    suspend fun getAllObjects(): List<KuladigObject> = kuladigDao.getAll()

    suspend fun getObjectById(id: String): KuladigObject? = kuladigDao.getById(id)

    suspend fun getAllObjectsWithProjects(): List<KuladigObjectWithProjects> =
        kuladigDao.getAllWithProjects()

    suspend fun getObjectByIdWithProjects(id: String): KuladigObjectWithProjects? =
        kuladigDao.getByIdWithProjects(id)

    suspend fun insertObject(kuladigObject: KuladigObject) = kuladigDao.insert(kuladigObject)

    suspend fun insertAllObjects(kuladigObjects: List<KuladigObject>) =
        kuladigDao.insertAll(kuladigObjects)

    suspend fun getAllProjects(): List<Project> = projectDao.getAll()

    suspend fun getProjectById(projektId: Int): Project? = projectDao.getById(projektId)

    suspend fun insertProject(project: Project) = projectDao.insert(project)

    suspend fun insertAllProjects(projects: List<Project>) = projectDao.insertAll(projects)

    suspend fun insertCrossRef(crossRef: com.example.kuladig_app.data.model.KuladigObjectProjectCrossRef) =
        crossRefDao.insert(crossRef)

    suspend fun insertAllCrossRefs(crossRefs: List<com.example.kuladig_app.data.model.KuladigObjectProjectCrossRef>) =
        crossRefDao.insertAll(crossRefs)

    suspend fun deleteAllData() {
        kuladigDao.deleteAll()
        projectDao.deleteAll()
        crossRefDao.deleteAll()
    }

    suspend fun isDatabaseEmpty(): Boolean {
        return kuladigDao.getObjectCount() == 0
    }

    // Tour-Methoden
    suspend fun getAllTours(): List<Tour> = tourDao.getAll()

    suspend fun getTourWithStops(tourId: Long): Pair<Tour?, List<TourStop>> {
        val tour = tourDao.getById(tourId)
        val stops = if (tour != null) tourStopDao.getStopsByTourId(tourId) else emptyList()
        return Pair(tour, stops)
    }

    suspend fun insertTour(tour: Tour, stops: List<TourStop>): Long {
        val tourId = tourDao.insert(tour)
        val stopsWithTourId = stops.map { it.copy(tourId = tourId) }
        tourStopDao.insertAll(stopsWithTourId)
        return tourId
    }

    suspend fun updateTour(tour: Tour, stops: List<TourStop>) {
        tourDao.update(tour)
        tourStopDao.deleteByTourId(tour.id)
        val stopsWithTourId = stops.map { it.copy(tourId = tour.id) }
        tourStopDao.insertAll(stopsWithTourId)
    }

    suspend fun deleteTour(tourId: Long) {
        val tour = tourDao.getById(tourId)
        if (tour != null) {
            tourStopDao.deleteByTourId(tourId)
            tourDao.delete(tour)
        }
    }
}

