package com.example.geo_tracker;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

/**
 * Activity displaying all existing reminder data
 */
public class ReminderData extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_data);

        // List view containing all reminder data
        ListView listView = findViewById(R.id.listView);

        // Instantiate reminder database
        ReminderDatabase reminderDatabase = ReminderDatabase.getDatabase(getApplicationContext());
        // Create data access object
        ReminderDao reminderDao = reminderDatabase.reminderDao();

        // Run on database thread
        ReminderDatabase.databaseWriteExecutor.execute(() -> {
            List<Reminder> reminderData = reminderDao.getAllReminders();
            runOnUiThread(() -> {
                // Fit adapter with data into list view
                ReminderListAdapter adapter = new ReminderListAdapter(getApplicationContext(), reminderData);
                listView.setAdapter(adapter);
            });
        });

        // Define quit button
        Button quitButton = findViewById(R.id.quit_button);
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                // Leave activity
                finish();
            }
        });
    }
}