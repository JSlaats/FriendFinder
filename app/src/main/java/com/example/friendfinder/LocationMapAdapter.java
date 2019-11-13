package com.example.friendfinder;

import android.app.Activity;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Map;

public class LocationMapAdapter {

    private static final String TAG = LocationMapAdapter.class.getName();

    private Marker marker;
    private Activity activity;
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;

    public LocationMapAdapter(Activity activity, GoogleMap map) {
        this.activity = activity;
        this.map = map;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
    }

    public GoogleMap getMap() {
        return map;
    }

    public void getLocation(){
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(activity, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    // Got last known location. In some rare situations this can be null.
                    if (location != null) {
                        // Logic to handle location object
                        doUiUpdate(new LatLng(location.getLatitude(),location.getLongitude()));
                    }
                }
            });
    }

    public void startLocationUpdates() {
        final LocationRequest locationRequest = LocationRequest.create().setInterval(5000).setFastestInterval(2500).setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    lastLocation = location;
                    doUiUpdate(new LatLng(location.getLatitude(),location.getLongitude()));

                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    private void doUiUpdate(final LatLng location) {
        Log.v(TAG,location.toString());
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (marker != null) {
                    marker.setPosition(location);


                } else {
                    marker = map.addMarker(new MarkerOptions().position(location).title("My Location"));

                }

            }
        });
    }

    public Location getLastLocation() {
        return lastLocation;
    }
}
