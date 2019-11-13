package com.example.friendfinder;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;


public class ArrowViewModel extends ViewModel implements SensorEventListener {

    private static final String TAG = ArrowViewModel.class.getName();
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private MapsActivity mapsActivity;

    public ArrowViewModel() {

    }

    public void initSensors(MapsActivity activity) {
        Log.v(TAG,"Initiating sensors: ");
        this.mapsActivity = activity;

        mSensorManager = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

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



    float[] mGravity;
    float[] mGeomagnetic;

    private float direction;

    @SuppressLint("DefaultLocale")
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {

                // orientation contains azimut, pitch and roll
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                float azimut = orientation[0];
                float angle = -azimut * 360 / (2 * 3.14159f);
                if(angle < 0){
                    angle += 360;
                }
                float shortenedRotation = Float.parseFloat(String.format("%.0f",angle));

                if(shortenedRotation != this.direction){
                    if(Math.abs(shortenedRotation - this.direction) >= 4) {
                        this.direction = shortenedRotation;

                        Log.v(TAG, "changed direction to " + this.direction);
                        mapsActivity.getArrowFragment().updateArrow(direction);
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.v(TAG,"onAccuracyChanged: "+accuracy);

    }


}
