package com.example.geo_tracker;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Declare cycle entity in database
 */
@Entity
public class Cycle
{
    // Declare database columns
    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "Title")
    private String title;

    @NonNull
    @ColumnInfo(name = "Time")
    private String time;

    // Entity constructor
    public Cycle(@NonNull String title, @NonNull String time)
    {
        this.title = title;
        this.time = time;
    }

    // Declare getters and setters
    @NonNull
    public String getTitle()
    {
        return title;
    }

    public void setTitle(@NonNull String title)
    {
        this.title = title;
    }

    @NonNull
    public String getTime()
    {
        return time;
    }

    public void setTime(@NonNull String time)
    {
        this.time = time;
    }
}
