package com.example.geo_tracker;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Cycling data access object
 */
@Dao
public interface CyclingDao
{
    // Insert cycle data unit into database
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Cycle cycle);

    // Fetch all cycle data from database
    @Query("SELECT * FROM cycle")
    List<Cycle> getAllCycles();
}
