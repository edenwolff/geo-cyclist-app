package com.example.geo_tracker;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.maps.model.LatLng;

/**
 * Cycling service class
 */
public class CyclingService extends Service {

    // Declare handler to post timer updates
    private Handler timerHandler;

    // Declare runnable thread
    private Runnable timerRunnable;

    // Time elapsed in cycle
    private int elapsedTime;
    private final IBinder binder = new LocalBinder();
    private boolean isTimerPaused = false, arrivedAtDestination = false;
    private ReminderDatabase db;
    private ReminderDao reminderDao;

    /**
     * Method is called when cyclist pauses cycle
     */
    public void pauseCyclingSession()
    {
        isTimerPaused = true;
    }

    /**
     * Resume cycle
     */
    public void resumeCyclingSession()
    {
        // Unpause timer
        isTimerPaused = false;
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    /**
     * Inner binder class
     */
    public class LocalBinder extends Binder
    {
        CyclingService getService()
        {
            return CyclingService.this;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (timerHandler != null) {
            // Stop the timer when the service is destroyed
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    /**
     *  Method to check if the cyclist is nearing a location reminder
     * @param currentLocation - cyclist's current location
     */
    private void checkLocationReminders(Location currentLocation)
    {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        ReminderDatabase.databaseWriteExecutor.execute(() -> {
                    // Loop through reminders list
                    for (Reminder reminder : reminderDao.getAllReminders())
                    {
                            Location reminderLocation = new Location(reminder.getTitle());
                            // Get the reminder latitude and longitude
                            reminderLocation.setLatitude(reminder.getLatitude());
                            reminderLocation.setLongitude(reminder.getLongitude());

                            // Calculate distance between current location and reminder location
                            float distanceToReminder = currentLocation.distanceTo(reminderLocation);

                            // If distance is less or equal to 10 meters then trigger notification
                            if (distanceToReminder <= 10.0)
                            {
                                // Create an explicit intent for CyclingActivity
                                Intent resultIntent = new Intent(this, CyclingActivity.class);
                                PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE);

                                // Create a reminder notification
                                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channelId")
                                        .setSmallIcon(R.drawable.ic_launcher_background)
                                        .setContentTitle(reminder.getTitle())
                                        .setContentText(reminder.getDescription())
                                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                                        .setAutoCancel(false);

                                builder.setContentIntent(resultPendingIntent);

                                // Check notification permission
                                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }

                                notificationManager.notify(1, builder.build());
                            }
                        }

                });
    }

    /**
     *  Method to check if user is nearing destination or location reminders
     */
    private void logMovement(Location currentLocation, Location destination) {
        // Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "channelId",
                    "Cycling Update",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Check if cyclist is nearing destination
        if (destination != null)
        {
            float distance = currentLocation.distanceTo(destination);

            if (distance <= 10.0 && !arrivedAtDestination)
            {
                arrivedAtDestination = true;

                // Create an "arrived at destination" notification
                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "channelId")
                        .setSmallIcon(R.drawable.ic_launcher_background)
                        .setContentTitle("Arrived at destination!")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

                // Create an explicit intent for CyclingActivity
                Intent resultIntent = new Intent(this, CyclingActivity.class);
                PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_IMMUTABLE);

                builder.setContentIntent(resultPendingIntent);

                // Check notification permission
                if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                {
                    return;
                }
                notificationManager.notify(1, builder.build());
            }
        }

        checkLocationReminders(currentLocation);
    }

    /**
     * Function is called when cycling session begins
     * @param currentLocation - current location parameter
     * @param destination - destination initially set by user
     */
    public void startCyclingSession(Location currentLocation, Location destination)
    {
        // Initialise the ReminderDatabase and ReminderDao
        db = ReminderDatabase.getDatabase(getApplicationContext());
        reminderDao = db.reminderDao();

        // Initialise variables for elapsed time tracking
        elapsedTime = 0;

        // Create a Handler to manage the timer thread
        timerHandler = new Handler();

        // Create a Runnable to define the behaviour of the timer thread
        timerRunnable = new Runnable() {
            @Override
            // Run method of the timer thread
            public void run() {
                // Check if the timer is not paused
                if (!isTimerPaused) {
                    // Update the elapsed time every second
                    elapsedTime += 1000;

                    // Send the elapsed time to the activity
                    sendElapsedTimeToActivity(elapsedTime);

                    // Schedule the next update after 1000 milliseconds (1 second)
                    timerHandler.postDelayed(this, 1000);

                    // Log movement based on current location and destination
                    logMovement(currentLocation, destination);
                } else {
                    // If the timer is paused, still send the elapsed time to the activity
                    sendElapsedTimeToActivity(elapsedTime);
                }
            }
        };

        // Start the timer by posting the initial delay of 1000 milliseconds (1 second)
        timerHandler.postDelayed(timerRunnable, 1000);

    }

    /**
     * This function broadcasts the updated time to the cycling activity
     * and is then displayed at the top of the page
     * @param time - updated time value parameter
     */
    private void sendElapsedTimeToActivity(int time) {
        // Send the updated time to the CyclingActivity for display
        Intent intent = new Intent("CyclingTimeUpdate");
        intent.putExtra("elapsedTime", time);
        sendBroadcast(intent);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}