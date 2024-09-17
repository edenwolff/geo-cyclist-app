package com.example.geo_tracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Activity which creates new reminder
 */
public class ReminderActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap gMap;
    private MapView mapView;
    private Button setReminder;
    private EditText reminderLabel, reminderDescription;
    private boolean isLocationSpecified = false;
    private ReminderDatabase db;
    private ReminderDao reminderDao;
    private MyLocationListener locationListener;
    private LocationManager locationManager;
    private double remLatitude, remLongitude;
    private BroadcastReceiver locationUpdateReceiver;
    private Location currentLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        // Instantiate location listener and manager
        locationListener = new MyLocationListener(getApplicationContext());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Instantiate BroadcastReceiver which receives live location updates
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                double latitude = intent.getDoubleExtra("latitude", 0.0);
                double longitude = intent.getDoubleExtra("longitude", 0.0);
                // Update the cyclist's position on the map
                currentLocation.setLatitude(latitude);
                currentLocation.setLongitude(longitude);

                // Store current location in new variable
                LatLng newLatLng = new LatLng(latitude, longitude);

                // Render locations on map - new current location, existing reminders and chosen destination
                renderLocationsOnMap(newLatLng);
            }
        };
        // Register location update receiver
        registerReceiver(locationUpdateReceiver, new IntentFilter("LocationUpdate"));

        // Instantiate map view
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        // Define reminder text field
        reminderLabel = findViewById(R.id.label_reminder);
        // Define reminder description text box
        reminderDescription = findViewById(R.id.description);

        db = ReminderDatabase.getDatabase(getApplicationContext());
        reminderDao = db.reminderDao();

        setReminder = findViewById(R.id.set_reminder);
        setReminder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(reminderLabel.getText().toString().isEmpty())
                {
                    // Reminder title is empty, show an alert dialog
                    new AlertDialog.Builder(ReminderActivity.this)
                            .setTitle("Alert")
                            .setMessage("Reminder title is empty")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                }

                // Reminder title exists, but check for description
                else
                {
                    if(reminderDescription.getText().toString().isEmpty())
                    {
                        // Ask user to confirm if they would like to set reminder with/without description
                        new AlertDialog.Builder(ReminderActivity.this)
                                .setTitle("Confirmation")
                                .setMessage("Set this reminder without a description?")
                                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        // Check if user has marked location
                                        if(isLocationSpecified)
                                        {
                                            // Add reminder without description
                                            insertReminder();
                                        }
                                    }
                                })
                                // Dismiss dialog so user can enter description
                                .setNegativeButton("No", new DialogInterface.OnClickListener()
                                {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which)
                                    {
                                        dialog.dismiss();
                                    }
                                })
                                .show();
                    }

                    else
                    {
                        // Check if user has marked location
                        if(isLocationSpecified)
                        {
                            // Add reminder with valid title and description
                            insertReminder();
                        }
                    }
                }
            }
        });


        // Define cancel button
        Button cancelButton = findViewById(R.id.cancel);

        // Create listener for a click
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            // Set result to cancel and leave activity
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
    }

    /**
     * Function which inserts new reminder into room database
     */
    private void insertReminder()
    {
        // Run on reminder database thread
        ReminderDatabase.databaseWriteExecutor.execute(() -> {
            // Create new reminder with set text, description and selected location
            Reminder newReminder = new Reminder(reminderLabel.getText().toString(), reminderDescription.getText().toString(), remLatitude, remLongitude);
            reminderDao.insert(newReminder);

            // Show success toast on UI thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ReminderActivity.this, "Reminder added successfully", Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Leave activity
        finish();
    }
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        gMap = googleMap;

        // Enable zoom controls
        gMap.getUiSettings().setZoomControlsEnabled(true);

        // Instantiate location manager
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Request permissions
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Fetch current location
        currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(currentLocation != null)
        {
            // Fetch latitude and longitude of current location
            LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            // Zoom camera into current location
            gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16.0f));

            // Render current location to map
            renderLocationsOnMap(currentLatLng);
        }
        // Set up map click listener
        gMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng)
            {
                // Reset map
                gMap.clear();

                // Define Latlng with current location coordinates and draw on map along with reminders
                LatLng currentLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                renderLocationsOnMap(currentLatLng);

                // Add marker on map after user click
                isLocationSpecified = true;
                gMap.addMarker(new MarkerOptions().position(latLng).title(reminderLabel.getText().toString()));
                remLatitude = latLng.latitude;
                remLongitude = latLng.longitude;
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();

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
     * @param currentLatLng - Current cyclist latitude and longitude
     * Function renders locations on the map - current cyclist location, destination,
     */
    private void renderLocationsOnMap(LatLng currentLatLng)
    {
        if (gMap != null)
        {
            // Mark the cyclist's current location on the map
            gMap.clear();
            CircleOptions circleOptions = new CircleOptions()
                    .center(currentLatLng)
                    .radius(10)
                    .strokeColor(Color.BLUE) // Color of the circle border
                    .fillColor(Color.argb(70, 0, 0, 255)); // Fill color with transparency

            gMap.addCircle(circleOptions);
            gMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));

            // Mark previously set reminders on the map
            ReminderDatabase.databaseWriteExecutor.execute(() -> {

                // Loop through the list of existing reminders
                for (Reminder reminder : reminderDao.getAllReminders())
                {
                    // Fetch each reminder coordinates and draw red circle at that location
                    LatLng remLatLng = new LatLng(reminder.getLatitude(), reminder.getLongitude());
                    CircleOptions reminderCircle = new CircleOptions()
                            .center(remLatLng)
                            .radius(10)
                            .strokeColor(Color.RED); // Color of the circle border
                    runOnUiThread(() -> {
                        // Add red circle to map
                        gMap.addCircle(reminderCircle);
                    });
                }
            });
        }
    }
}