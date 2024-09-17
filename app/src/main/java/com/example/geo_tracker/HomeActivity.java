package com.example.geo_tracker;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Home activity containing buttons leading to all activities
 */
public class HomeActivity extends AppCompatActivity {

    // Define activity launcher
    ActivityResultLauncher<Intent> activityResultLauncher;

    // Define buttond
    Button newCycle;
    Button newReminder;
    Button cycleData;
    Button reminderData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialise all buttons in home activity
        newCycle = findViewById(R.id.new_cycle);
        newReminder = findViewById(R.id.new_reminder);
        cycleData = findViewById(R.id.cycling_data);
        reminderData = findViewById(R.id.reminder_data);

        /* Create a results contract for activity */
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {}
        );
    }

    /**
     * Switch to cycle activity
     * @param v
     */
    public void onCycleClick(View v)
    {
        Intent cycle = new Intent(HomeActivity.this, CyclingActivity.class);
        activityResultLauncher.launch(cycle);
    }

    /**
     * Switch to reminder activity
     * @param v
     */
    public void onNewReminderClick(View v)
    {
        Intent setReminder = new Intent(HomeActivity.this, ReminderActivity.class);
        activityResultLauncher.launch(setReminder);
    }

    /**
     * Switch to cycling data activity
     * @param v
     */
    public void onCyclingDataClick(View v)
    {
        Intent viewCyclingData = new Intent(HomeActivity.this, CyclingData.class);
        activityResultLauncher.launch(viewCyclingData);
    }

    /**
     * Switch to reminder activity
     * @param v
     */
    public void onReminderDataClick(View v)
    {
        Intent viewReminderData = new Intent(HomeActivity.this, ReminderData.class);
        activityResultLauncher.launch(viewReminderData);
    }
}