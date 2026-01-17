package com.example.kuladig_app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.kuladig_app.data.model.Tour

@Dao
interface TourDao {
    @Query("SELECT * FROM tours ORDER BY createdAt DESC")
    suspend fun getAll(): List<Tour>

    @Query("SELECT * FROM tours WHERE id = :id")
    suspend fun getById(id: Long): Tour?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tour: Tour): Long

    @Update
    suspend fun update(tour: Tour)

    @Delete
    suspend fun delete(tour: Tour)
}
