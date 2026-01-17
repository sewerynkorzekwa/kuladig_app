package com.example.kuladig_app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.kuladig_app.data.model.TourStop

@Dao
interface TourStopDao {
    @Query("SELECT * FROM tour_stops WHERE tourId = :tourId ORDER BY stopOrder ASC")
    suspend fun getStopsByTourId(tourId: Long): List<TourStop>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stop: TourStop): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stops: List<TourStop>)

    @Query("DELETE FROM tour_stops WHERE tourId = :tourId")
    suspend fun deleteByTourId(tourId: Long)

    @Update
    suspend fun update(stop: TourStop)

    @Delete
    suspend fun delete(stop: TourStop)
}
