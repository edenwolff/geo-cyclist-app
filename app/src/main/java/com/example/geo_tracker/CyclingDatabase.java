package com.example.geo_tracker;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Cycling room database class
 */
@Database(entities = {Cycle.class}, version = 1, exportSchema = false)
public abstract class CyclingDatabase extends RoomDatabase
{
    // Define thread count
    private static final int threadCount = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(threadCount);

    public abstract CyclingDao cyclingDao();
    private static volatile CyclingDatabase instance;

    static CyclingDatabase getDatabase(final Context context) {
        if (instance == null) {
            synchronized (CyclingDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    CyclingDatabase.class, "cycling_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
