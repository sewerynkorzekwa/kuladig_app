package com.example.kuladig_app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.kuladig_app.data.model.Project

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects")
    suspend fun getAll(): List<Project>

    @Query("SELECT * FROM projects WHERE projektId = :projektId")
    suspend fun getById(projektId: Int): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<Project>)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()
}

