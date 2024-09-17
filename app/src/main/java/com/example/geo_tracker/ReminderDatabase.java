package com.example.geo_tracker;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reminder room database class
 */
@Database(entities = {Reminder.class}, version = 1, exportSchema = false)
public abstract class ReminderDatabase extends RoomDatabase
{
    private static final int threadCount = 4;
    static final ExecutorService databaseWriteExecutor = Executors.newFixedThreadPool(threadCount);
    public abstract ReminderDao reminderDao();
    private static volatile ReminderDatabase instance;

    /**
     * Gety reminder room database
     * @param context - application context
     * @return - database intance
     */
    static ReminderDatabase getDatabase(final Context context) {
        if (instance == null) {
            synchronized (ReminderDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                                    ReminderDatabase.class, "reminder_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}
