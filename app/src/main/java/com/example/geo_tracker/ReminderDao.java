package com.example.geo_tracker;

import java.util.List;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

/**
 * Reminder data access object interface
 */
@Dao
public interface ReminderDao
{
    // Insert reminder data unit into database
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Reminder reminder);

    // Fetch all reminder data from database
    @Query("SELECT * FROM reminders")
    List<Reminder> getAllReminders();
}
