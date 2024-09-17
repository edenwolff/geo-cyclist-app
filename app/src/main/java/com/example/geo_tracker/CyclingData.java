package com.example.geo_tracker;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

/**
 * Activity displaying all the existing cycling data
 */
public class CyclingData extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycling_data);

        // List view containing all cycling data
        ListView listView = findViewById(R.id.list_view);

        // Instantiate cycling database
        CyclingDatabase cyclingDatabase = CyclingDatabase.getDatabase(getApplicationContext());
        // Create data access object
        CyclingDao cyclingDao = cyclingDatabase.cyclingDao();

        // Run on database thread
        CyclingDatabase.databaseWriteExecutor.execute(() -> {
            List<Cycle> cyclingData = cyclingDao.getAllCycles();
            runOnUiThread(() -> {
                // Fit adapter with data into list view
                CycleListAdapter adapter = new CycleListAdapter(getApplicationContext(), cyclingData);
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
