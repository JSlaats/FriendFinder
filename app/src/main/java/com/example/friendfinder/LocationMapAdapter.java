package com.example.friendfinder;

import android.app.Activity;
import android.location.Location;
import android.os.Looper;

import com.example.friendfinder.util.Util;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;

public class LocationMapAdapter {

    private static final String TAG = LocationMapAdapter.class.getName();

   // private Marker marker;
    private MainActivity activity;
    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastLocation;

    public LocationMapAdapter(Activity activity, GoogleMap map) {
        this.activity = (MainActivity) activity;
        this.map = map;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
    }


    public void startLocationUpdates() {
        final LocationRequest locationRequest = LocationRequest.create().setInterval(5000).setFastestInterval(2500).setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    setLastLocation(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void setLastLocation(Location lastLocation) {
        if(this.lastLocation == null) {
            map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(Util.LocationToLatLng(lastLocation)).zoom(11.0f).build()));
        }

        this.lastLocation = lastLocation;
        activity.syncLocation(lastLocation);
        try {
            activity.getArrowFragment().getmViewModel().setLastLocation(lastLocation);
        }catch(Exception ignored){}
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    /*
    private void doUiUpdate(final LatLng location) {
        //not needed anymore, no reason to put a marker on my location.
        Log.v(TAG,location.toString());
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (marker != null) {
                    marker.setPosition(location);
                } else {
                    marker = map.addMarker(new MarkerOptions().position(location).title("My Location"));
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(location).zoom(11.0f).build()));
                }

            }
        });
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
                        //doUiUpdate(new LatLng(location.getLatitude(),location.getLongitude()));
                       // map.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(Util.LocationToLatLng(location)).zoom(11.0f).build()));
                    }
                }
            });
    }*/

}
