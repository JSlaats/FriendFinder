package com.example.friendfinder.util;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

public class Util {
    public static LatLng LocationToLatLng(Location loc){
        if(loc == null)return null;
        return new LatLng(loc.getLatitude(),loc.getLongitude());
    }
}
