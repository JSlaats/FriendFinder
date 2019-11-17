package com.example.friendfinder.Fragments;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.lifecycle.ViewModel;

import com.example.friendfinder.MainActivity;
import com.google.android.gms.maps.model.LatLng;

//compass to coordinates: https://stackoverflow.com/questions/4308262/calculate-compass-bearing-heading-to-location-in-android
public class ArrowViewModel extends ViewModel implements SensorEventListener {

    private static final String TAG = ArrowViewModel.class.getName();
    private SensorManager mSensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private MainActivity activity;
    private float distance;
    private float[] mGravity;
    private float[] mGeomagnetic;
    private float heading;
    private LatLng activeMarker;
    private Location lastLocation;
    private LatLng myLocation;
    private static final float ALPHA = 0.20f;

    public ArrowViewModel() {

    }
    private HandlerThread mSensorThread;
    private Handler mSensorHandler;

    //start sensors
    void initSensors(MainActivity activity) {
        Log.v(TAG,"Initiating sensors: ");
        this.activity = activity;

        mSensorThread = new HandlerThread("Sensor thread", Thread.MAX_PRIORITY);
        mSensorThread.start();
        mSensorHandler = new Handler(mSensorThread.getLooper()); //Blocks until looper is prepared, which is fairly quick

        mSensorManager = (SensorManager)activity.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL,mSensorHandler);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL,mSensorHandler);

    }

    //stop sensors when app isnt in forground to save battery life
    void stopSensors(){
        Log.v(TAG,"Stopping sensors: ");
        mSensorManager.unregisterListener(this,accelerometer);
        mSensorManager.unregisterListener(this,magnetometer);
        mSensorThread.quitSafely();

    }





    private float calculateBearing(LatLng myPosition, LatLng friendPosition) {
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

    private float calculateAngle(float heading, float bearing, GeomagneticField geoField){
        heading = (bearing - heading) * -1;
        return Math.round(normalizeDegree(heading));
    }

    private float normalizeDegree(float value) {
        if (value >= 0.0f && value <= 180.0f) {
            return value;
        } else {
            return 180 + (180 + value);
        }
    }


    /*
     * ti≤ alpha ≤ 1 ; a smaller value basically means more smoothing
    // htme smoothing constant for low-pass filter
    //     * 0 tp://blog.thomnichols.org/2011/08/smoothing-sensor-data-with-a-low-pass-filter
    // http://blog.thomnichols.org/2012/06/smoothing-sensor-data-part-2
     */

    private float[] lowPass(float[] input, float[] output) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }



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
                    //if(Math.abs(Float.parseFloat(String.format("%.0f",angle)) - Float.parseFloat(String.format("%.0f",this.heading))) >= 0.5) {
                        this.heading = angle;
                        calculateArrow(heading);
                    //}
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.v(TAG,"onAccuracyChanged: "+accuracy);

    }

    public void setActiveMarker(LatLng activeMarker) {
        this.activeMarker = activeMarker;
    }

    public void setLastLocation(Location lastLocation) {
        this.lastLocation = lastLocation;
        this.myLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
    }

    private void calculateArrow(float heading){
        if(activeMarker != null) {
            //calculate distance
            final float[] result = new float[1];
            Location.distanceBetween(lastLocation.getLatitude(),lastLocation.getLongitude(),activeMarker.latitude,activeMarker.longitude,result);
            if(result[0] != this.distance) {
                this.distance = result[0];

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.getArrowFragment().updateDistanceLabel(String.format("%.0f", result[0]));
                    }
                });
            }
            //calculate bearing
            float bearing = calculateBearing(myLocation, activeMarker);

            GeomagneticField geoField = new GeomagneticField(
                    Double.valueOf(lastLocation.getLatitude()).floatValue(),
                    Double.valueOf(lastLocation.getLongitude()).floatValue(),
                    Double.valueOf(lastLocation.getAltitude()).floatValue(),
                    System.currentTimeMillis()
            );
            heading += geoField.getDeclination();

            final float rotation = calculateAngle(heading,bearing,geoField);

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.getArrowFragment().updateArrow(rotation);
                }
            });
        }
    }
    public static LatLng midPoint(LatLng myLocation,LatLng targetLocation){
        double lat = (myLocation.latitude+targetLocation.latitude)/2;
        double lon = (myLocation.longitude+targetLocation.longitude)/2;
        return new LatLng(lat,lon);
    }
//    public float getAngle(LatLng myPosition, LatLng friendPosition){
//        float angle = (float) Math.toDegrees(
//                Math.atan2(
//                        myPosition.latitude - friendPosition.latitude,
//                        myPosition.longitude - friendPosition.longitude
//                )
//        );
//
//        if(angle < 0){
//            angle += 360;
//        }
//
//        return angle;
//    }
}
