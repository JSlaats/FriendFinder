package com.example.friendfinder.util;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;

import java.util.Map;
import java.util.Objects;

public class Util {
    public static LatLng LocationToLatLng(Location loc){
        if(loc == null)return null;
        return new LatLng(loc.getLatitude(),loc.getLongitude());
    }
    public static LatLng GeoPointToLatLng(GeoPoint loc){
        if(loc == null)return null;
        return new LatLng(loc.getLatitude(),loc.getLongitude());
    }
    public static GeoPoint LatLngToGeoPoint(LatLng loc){
        if(loc == null)return null;
        return new GeoPoint(loc.latitude,loc.longitude);
    }

    public static <T, E> T getKeyByValue(Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

}
