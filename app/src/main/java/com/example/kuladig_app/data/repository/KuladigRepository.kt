package com.example.kuladig_app.data.repository

import com.example.kuladig_app.data.dao.KuladigDao
import com.example.kuladig_app.data.dao.KuladigObjectProjectCrossRefDao
import com.example.kuladig_app.data.dao.ProjectDao
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.KuladigObjectWithProjects
import com.example.kuladig_app.data.model.Project

class KuladigRepository(
    private val kuladigDao: KuladigDao,
    private val projectDao: ProjectDao,
    private val crossRefDao: KuladigObjectProjectCrossRefDao
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
}

