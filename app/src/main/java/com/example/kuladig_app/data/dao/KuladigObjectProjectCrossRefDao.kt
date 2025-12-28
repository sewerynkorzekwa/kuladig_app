package com.example.kuladig_app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kuladig_app.data.model.KuladigObjectProjectCrossRef

@Dao
interface KuladigObjectProjectCrossRefDao {
    @Query("SELECT * FROM kuladig_object_project_cross_ref")
    suspend fun getAll(): List<KuladigObjectProjectCrossRef>

    @Query("SELECT projectId FROM kuladig_object_project_cross_ref WHERE kuladigObjectId = :kuladigObjectId")
    suspend fun getProjectIdsForObject(kuladigObjectId: String): List<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(crossRef: KuladigObjectProjectCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(crossRefs: List<KuladigObjectProjectCrossRef>)

    @Query("DELETE FROM kuladig_object_project_cross_ref")
    suspend fun deleteAll()
}

