package com.example.friendfinder.Fragments;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.example.friendfinder.MapsActivity;
import com.google.android.gms.maps.model.LatLng;

//compass to coordinates: https://stackoverflow.com/questions/4308262/calculate-compass-bearing-heading-to-location-in-android
public class ArrowViewModel extends ViewModel implements SensorEventListener {

    private static final String TAG = ArrowViewModel.class.getName();
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private MapsActivity mapsActivity;

    public ArrowViewModel() {

    }

    //start sensors
    public void initSensors(MapsActivity activity) {
        Log.v(TAG,"Initiating sensors: ");
        this.mapsActivity = activity;

        mSensorManager = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    //stop sensors when app isnt in forground to save battery life
    public void stopSensors(){
        Log.v(TAG,"Stopping sensors: ");
        mSensorManager.unregisterListener(this,accelerometer);
        mSensorManager.unregisterListener(this,magnetometer);
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



    public float calculateBearing(LatLng myPosition, LatLng friendPosition) {
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
        brng = 360 - brng; // count degrees counter-clockwise - remove to make clockwise

        return (float)brng;
    }

    public float calculateAngle(float heading, float bearing, GeomagneticField geoField){

        heading = (bearing - heading) * -1;

       // heading = bearing - (bearing + heading);
        return Math.round(normalizeDegree(heading));
    }
    private float normalizeDegree(float value) {
        if (value >= 0.0f && value <= 180.0f) {
            return value;
        } else {
            return 180 + (180 + value);
        }
    }

    // http://blog.thomnichols.org/2011/08/smoothing-sensor-data-with-a-low-pass-filter
    // http://blog.thomnichols.org/2012/06/smoothing-sensor-data-part-2
    /*
     * time smoothing constant for low-pass filter
     * 0 ≤ alpha ≤ 1 ; a smaller value basically means more smoothing
     * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
     */
    static final float ALPHA = 0.15f;

    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    private float[] mGravity;
    private float[] mGeomagnetic;

    private float heading;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = lowPass( event.values.clone(), mGravity );

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = lowPass( event.values.clone(),mGeomagnetic);

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {

                // orientation contains azimut, pitch and roll
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                float azimut = orientation[0];
                float angle = -azimut * 360 / (2 * 3.14159f);
                if(angle < 0){ angle += 360; }

                if(angle != this.heading){
                    //stop the overload of data
                    if(Math.abs(Float.parseFloat(String.format("%.0f",angle)) - Float.parseFloat(String.format("%.0f",this.heading))) >= 4) {
                        this.heading = angle;
                        mapsActivity.getArrowFragment().updateArrow(heading);
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
