package com.example.friendfinder;

import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

public class ArrowViewModel extends ViewModel {
    // TODO: Implement the ViewModel

    public ArrowViewModel() {
    }

    public float getAngle(LatLng myPosition, LatLng friendPosition){
        float angle = (float) Math.toDegrees(
                Math.atan2(
                        myPosition.latitude - friendPosition.latitude,
                        myPosition.longitude - friendPosition.longitude
                )
        );

        if(angle < 0){
            angle += 360;
        }

        return angle;
    }

    public float angleFromCoordinate(LatLng myPosition, LatLng friendPosition) {

        double lat1= myPosition.latitude;
        double long1= myPosition.longitude;
        double lat2= friendPosition.latitude;
        double long2= friendPosition.longitude;

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;
        //brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise

        return (float)brng;
    }

}
