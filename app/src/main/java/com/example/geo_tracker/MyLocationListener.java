package com.example.geo_tracker;

import android.location.Location;
import android.location.LocationListener;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

/**
 * My location listener class which broadcasts live locations to activities
 */
public class MyLocationListener implements LocationListener
{
    private Context context;
    public MyLocationListener(Context context) {
        this.context = context;
    }

    /**
     * Send live coordinates as soon as location changed
     */
    @Override
    public void onLocationChanged(@NonNull Location location)
    {
        // Send a broadcast with the new location
        Intent intent = new Intent("LocationUpdate");
        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        context.sendBroadcast(intent);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras)
    {
        Log.d("comp3018", "onStatusChanged: " + provider + " " + status);
    }
    @Override
    public void onProviderEnabled(String provider)
    {
        Log.d("comp3018", "onProviderEnabled: " + provider);
    }
    @Override
    public void onProviderDisabled(String provider)
    {
        Log.d("comp3018", "onProviderDisabled: " + provider);
    }

}
