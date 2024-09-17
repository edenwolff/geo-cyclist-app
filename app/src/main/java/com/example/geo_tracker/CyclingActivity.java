package com.example.geo_tracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

/**
 * Cycling activity - track user movement and triggers reminders and destination updates..
 */
public class CyclingActivity extends AppCompatActivity implements OnMapReadyCallback {

    /* Declare all activity components ad variables */
    private MapView mapView;
    private GoogleMap gMap;
    private Button startCycleButton, pauseCycleButton, endCycleButton, quitButton;
    private LocationManager locationManager;
    private MyLocationListener locationListener;
    private CyclingService cyclingService;
    private boolean isBound = false, isCycling = false;
    private TextView cyclingTimer;
    private EditText cycleTitle;
    private BroadcastReceiver timeUpdateReceiver, locationUpdateReceiver;
    private boolean isTimerPaused = false, isDestinationSelected = false;
    Location currentLocation, selectedDestination;
    private ReminderDatabase reminderDB;
    private CyclingDatabase cycleDB;
    private ReminderDao reminderDao;
    private CyclingDao cyclingDao;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cycling);

        // Initialise reminder and cycling room databases
        reminderDB = ReminderDatabase.getDatabase(getApplicationContext());
        cycleDB = CyclingDatabase.getDatabase(getApplicationContext());

        // Initialise reminder and cycling data access objects
        reminderDao = reminderDB.reminderDao();
        cyclingDao = cycleDB.cyclingDao();

        // Initialise map view which will track user movement
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Initialise location manager and listener
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new MyLocationListener(getApplicationContext());

        // Initialise all buttons (start cycle, pause cycle, end cycle, return to menu)
        startCycleButton = findViewById(R.id.start_button);
        pauseCycleButton = findViewById(R.id.pause_button);
        endCycleButton = findViewById(R.id.end_button);
        quitButton = findViewById(R.id.quit_button);

        // Create a listener for the start button
        startCycleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                // If cycle name is empty ask user to name it
                if(cycleTitle.getText().toString().isEmpty())
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(CyclingActivity.this);
                    builder.setMessage("Please title your cycle")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });

                    // Create alert dialog and show it
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else
                {
                    // If destination already selected, start cycling service
                    if (isDestinationSelected)
                    {
                        startCyclingService();
                    }
                    /* If no destination selected, ask user whether they would like to
                    start cycling without one */
                    else
                    {
                        // Show the destination confirmation dialog
                        showConfirmationDialog();
                    }
                }
            }
        });

        // Set pause button as disabled by default (Before cycle started)
        pauseCycleButton.setEnabled(false);
        // Create listener for a button click
        pauseCycleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Pause the service on click
                cyclingService.pauseCyclingSession();
                // Toggle pause button from pause to resume and vice versa..
                toggleTimerPauseState();
            }
        });

        // Set end cycle button as disabled by default (Before cycle started)
        endCycleButton.setEnabled(false);
        // Create button click listener
        endCycleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(isCycling)
                {
                    // Stop timer
                    cyclingService.pauseCyclingSession();

                    // enable back button & disable pause button because cycle has been ended
                    quitButton.setEnabled(true);

                    // Stop service because cycle has ended
                    stopCyclingService();
                    // Create alert dialog asking if user wants to save cycle data
                    AlertDialog.Builder builder = new AlertDialog.Builder(CyclingActivity.this);
                    builder.setMessage("Save cycle data?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    // Add data an disable buttons
                                    addCycleData();
                                    pauseCycleButton.setEnabled(false);
                                    endCycleButton.setEnabled(false);
                                }
                            })
                            .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id)
                                {
                                    dialog.dismiss();
                                    pauseCycleButton.setEnabled(false);
                                    endCycleButton.setEnabled(false);
                                }
                            });
                    // Create the AlertDialog object and show it
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            }
        });

        // Create button listener for the quit button
        quitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Leave activity
                finish();
            }
        });

        // Initialise page title and timer
        cyclingTimer = findViewById(R.id.cycling_timer);
        cycleTitle = findViewById(R.id.label_cycle);

        // Register a BroadcastReceiver for receiving time updates
        timeUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // Extract the elapsed time from the intent
                int elapsedTime = intent.getIntExtra("elapsedTime", 0);

                // Update the timer using the received elapsed time
                updateTimer(elapsedTime);
            }
        };
        // Register the timeUpdateReceiver with an IntentFilter for "CyclingTimeUpdate" actions
        registerReceiver(timeUpdateReceiver, new IntentFilter("CyclingTimeUpdate"));

        // Register a BroadcastReceiver for receiving location updates
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                // Fetch live latitude and longitude that are being broadcast
                double latitude = intent.getDoubleExtra("latitude", 0.0);
                double longitude = intent.getDoubleExtra("longitude", 0.0);

                // Update the cyclist's position on the map
                currentLocation.setLatitude(latitude);
                currentLocation.setLongitude(longitude);

                // Create new LatLng with live coordinates
                LatLng newLatLng = new LatLng(latitude, longitude);

                // Render updated location on map
                renderLocationsOnMap(newLatLng);
            }
        };
        // Register location update BroadcastReceiver to receive live location updates
        registerReceiver(locationUpdateReceiver, new IntentFilter("LocationUpdate"));
    }

    /**
     * Ask user to confirm cycling without pre-defined destination
     */
    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Start cycling without a destination?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked Yes, start cycling without a destination
                        startCyclingService();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked No, do nothing
                        dialog.dismiss();
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Updates the cycling timer with the elapsed time.
     *
     * @param time Elapsed time in milliseconds.
     */
    private void updateTimer(int time) {
        // Calculate elapsed minutes and seconds from the total time in milliseconds
        int elapsedMinutes = time / 60000;
        int elapsedSeconds = (time % 60000) / 1000;

        // Format the elapsed time as a string with leading zeros
        String elapsedTimeString = String.format(Locale.getDefault(), "%02d:%02d", elapsedMinutes, elapsedSeconds);

        // Set the formatted elapsed time to the cycling timer TextView
        cyclingTimer.setText(elapsedTimeString);
    }

    private void toggleTimerPauseState() {
        isTimerPaused = !isTimerPaused;

        if (isTimerPaused) {
            // Pause the timer
            pauseCycleButton.setText("Resume Cycle"); // Change button text to "Resume"
            cyclingService.pauseCyclingSession();
        } else {
            // Resume the timer
            pauseCycleButton.setText("Pause Cycle"); // Change button text to "Pause"
            cyclingService.resumeCyclingSession();
        }
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("Test", "SERVICE CONNECTED");
            // Cast the IBinder and get the CyclingService instance
            CyclingService.LocalBinder binder = (CyclingService.LocalBinder) service;
            cyclingService = binder.getService();
            // Set bound variable to true when connecting
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.d("Test", "SERVICE DISCONNECTED");
            // Set bound variable to false when disconnecting
            isBound = false;
        }
    };

    /**
     * Method called when cycling session is started
     */
    private void startCyclingService()
    {
        // Disable and enable relevant buttons
        startCycleButton.setEnabled(false);
        endCycleButton.setEnabled(true);
        pauseCycleButton.setEnabled(true);
        quitButton.setEnabled(false);

        // Create intent to start cycling service
        Intent serviceIntent = new Intent(this, CyclingService.class);
        // Start the service
        startService(serviceIntent);

        // Start cycling!
        isCycling = true;
        cyclingService.startCyclingSession(currentLocation, selectedDestination);
    }

    // Called when cyclist stops cycling
    private void stopCyclingService() {
        Intent serviceIntent = new Intent(this, CyclingService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

        // Constantly listen for location updates from the location listener
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5,
                    2,    // minimum distance between updates, in meters
                    locationListener
            );
        } catch (SecurityException e) {
            Log.d("comp3018", e.toString());
        }
    }

    /**
     * Bind cycling service when activity is started
     */
    @Override
    public void onStart() {
        // Create new intent
        Intent intent = new Intent(CyclingActivity.this, CyclingService.class);
        super.onStart();

        // Bind service
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
    }


    @Override
    public void onPause() {
        // Call the superclass method for any additional cleanup
        super.onPause();

        // Pause the MapView
        mapView.onPause();

        // Remove location updates to conserve resources when the activity is paused
        locationManager.removeUpdates(locationListener);
    }

    @Override
    public void onDestroy() {
        // Call the superclass method for any additional cleanup
        super.onDestroy();

        // Destroy the MapView to release resources
        mapView.onDestroy();

        // Unregister the timeUpdateReceiver to avoid memory leaks
        unregisterReceiver(timeUpdateReceiver);
    }

    @Override
    public void onLowMemory() {
        // Call the superclass method to handle low memory situations
        super.onLowMemory();

        // Notify the MapView of low memory conditions
        mapView.onLowMemory();
    }


    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;
        // Enable zooming
        gMap.getUiSettings().setZoomControlsEnabled(true);

        // Set up map click listener
        gMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                // Save clicked location as the selected destination
                if (!isDestinationSelected && !isCycling) {

                    // Mark and save selected destination
                    gMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));
                    selectedDestination = new Location("Destination");
                    selectedDestination.setLatitude(latLng.latitude);
                    selectedDestination.setLongitude(latLng.longitude);
                    isDestinationSelected = true;
                }
            }
        });

        // Fetch Current Location
        //getCurrentLocation();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(currentLocation != null)
        {
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16.0f));

            renderLocationsOnMap(currentLatLng);
        }
    }

    /**
     * @param currentLatLng - Current cyclist latitude and longitude
     * Function renders locations on the map - current cyclist location, destination,
     */
    private void renderLocationsOnMap(LatLng currentLatLng)
    {
        // Verify map exists
        if (gMap != null)
        {
            // Clear previous markers and add a new circle at the updated position
            gMap.clear();

            // Mark current location as blue circle
            CircleOptions currentLocationCircle = new CircleOptions()
                    .center(currentLatLng)
                    .radius(10)
                    .strokeColor(Color.BLUE) // Color of the circle border
                    .fillColor(Color.argb(70, 0, 0, 255)); // Fill color with transparency

            // Add circle to map
            gMap.addCircle(currentLocationCircle);
            // Adjust camera angle to focus on the user's location
            gMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));

            // Add back the destination marker if it exists
            if (selectedDestination != null)
            {
                gMap.addMarker(new MarkerOptions().position(new LatLng(selectedDestination.getLatitude(), selectedDestination.getLongitude())).title("Destination"));
            }

            ReminderDatabase.databaseWriteExecutor.execute(() -> {

                for (Reminder reminder : reminderDao.getAllReminders())
                {
                    LatLng remLatLng = new LatLng(reminder.getLatitude(), reminder.getLongitude());
                    CircleOptions reminderCircle = new CircleOptions()
                            .center(remLatLng)
                            .radius(10)
                            .strokeColor(Color.RED); // Color of the circle border
                    runOnUiThread(() -> {
                        gMap.addCircle(reminderCircle);
                    });
                }
            });
        }
    }

    /**
     * Add saved cycle data to database
     * which can be viewed by cyclist
     */
    private void addCycleData()
    {
        // Perform database actions one of database threads
        CyclingDatabase.databaseWriteExecutor.execute(() -> {
            // Create new cycle object
            Cycle cycleData = new Cycle(cycleTitle.getText().toString(), cyclingTimer.getText().toString());
            // Insert it into database
            cyclingDao.insert(cycleData);

            // Create toast message on UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CyclingActivity.this, "Cycle data added successfully", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}