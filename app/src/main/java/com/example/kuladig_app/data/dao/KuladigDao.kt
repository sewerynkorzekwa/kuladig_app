package com.example.kuladig_app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.kuladig_app.data.model.KuladigObject
import com.example.kuladig_app.data.model.KuladigObjectWithProjects

@Dao
interface KuladigDao {
    @Query("SELECT * FROM kuladig_objects")
    suspend fun getAll(): List<KuladigObject>

    @Query("SELECT * FROM kuladig_objects WHERE id = :id")
    suspend fun getById(id: String): KuladigObject?

    @Query("SELECT COUNT(*) FROM kuladig_objects")
    suspend fun getObjectCount(): Int

    @Transaction
    @Query("SELECT * FROM kuladig_objects")
    suspend fun getAllWithProjects(): List<KuladigObjectWithProjects>

    @Transaction
    @Query("SELECT * FROM kuladig_objects WHERE id = :id")
    suspend fun getByIdWithProjects(id: String): KuladigObjectWithProjects?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(kuladigObject: KuladigObject)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(kuladigObjects: List<KuladigObject>)

    @Query("DELETE FROM kuladig_objects")
    suspend fun deleteAll()
}

