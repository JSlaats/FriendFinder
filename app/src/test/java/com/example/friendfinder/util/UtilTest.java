package com.example.friendfinder.util;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UtilTest {
    @Before
    public void setUp() throws Exception {

    }

//    @Test
//    public void locationToLatLng() {
//        Location location = new Location("");
//        location.setLongitude(1.1);
//        location.setLatitude(1.1);
//        LatLng result = Util.LocationToLatLng(location);
//        LatLng expectedResult = new LatLng(1.1,1.1);
//        Assert.assertEquals(result.latitude,expectedResult.latitude,0);
//        Assert.assertEquals(result.longitude,expectedResult.longitude,0);
//    }

    @Test
    public void geoPointToLatLng() {
        LatLng result = Util.GeoPointToLatLng(new GeoPoint(0.1,0.1));
        LatLng expectedResult = new LatLng(0.1,0.1);
        Assert.assertEquals(result.latitude,expectedResult.latitude,0);
        Assert.assertEquals(result.longitude,expectedResult.longitude,0);

    }

    @Test
    public void latLngToGeoPoint() {
        GeoPoint result = Util.LatLngToGeoPoint(new LatLng(0.1,0.1));
        GeoPoint expectedResult = new GeoPoint(0.1,0.1);
        Assert.assertEquals(result.getLatitude(),expectedResult.getLatitude(),0);
        Assert.assertEquals(result.getLongitude(),expectedResult.getLongitude(),0);

    }

}