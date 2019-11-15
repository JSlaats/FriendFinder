package com.example.friendfinder.Fragments;

import androidx.lifecycle.ViewModelProviders;

import android.hardware.GeomagneticField;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.friendfinder.MapsActivity;
import com.example.friendfinder.R;
import com.google.android.gms.maps.model.LatLng;

public class ArrowFragment extends Fragment  {

    private static final String TAG = ArrowFragment.class.getName();
    private ArrowViewModel mViewModel;
    private ImageView mImageViewArrow;
    private TextView mTxtPercentage;
    private TextView mTxtDistance;

    private MapsActivity activity;
    private float distance;

    public static ArrowFragment newInstance() {
        return new ArrowFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.arrow_fragment, container, false);

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ArrowViewModel.class);

        mImageViewArrow = getView().findViewById(R.id.imageViewArrow);
        mTxtPercentage = getView().findViewById(R.id.txtPercentage);
        mTxtDistance = getView().findViewById(R.id.txtDistance);

        this.activity = (MapsActivity) getActivity();
        activity.setArrowFragment(this);

        // TODO: Use the ViewModel

    }
//region lifecycle
    @Override
    public void onStart() {
        super.onStart();
        mViewModel.initSensors(activity);

    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.initSensors(activity);

    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.stopSensors();

    }

    @Override
    public void onStop() {
        super.onStop();
        mViewModel.stopSensors();

    }
//endregion
    void updateArrow(float heading){

        if(activity.getSelectedMarker() != null) {
            //get locations
            LatLng activeMarker = activity.getSelectedMarker().getPosition();
            Location lastLocation = activity.getLocationMapAdapter().getLastLocation();
            LatLng myLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());


            //calculate distance
            float[] result = new float[1];
            Location.distanceBetween(lastLocation.getLatitude(),lastLocation.getLongitude(),activeMarker.latitude,activeMarker.longitude,result);
            if(result[0] != this.distance) {
                this.distance = result[0];
                updateDistanceLabel(String.format("%.0f", result[0]));
            }
            //calculate bearing
            float bearing = mViewModel.calculateBearing(myLocation, activeMarker);

            GeomagneticField geoField = new GeomagneticField(
                    Double.valueOf(lastLocation.getLatitude()).floatValue(),
                    Double.valueOf(lastLocation.getLongitude()).floatValue(),
                    Double.valueOf(lastLocation.getAltitude()).floatValue(),
                    System.currentTimeMillis()
            );
            heading += geoField.getDeclination();

            float resultAngle = mViewModel.calculateAngle(heading,bearing,geoField);

            mImageViewArrow.setRotation(resultAngle);
        }
    }


    void updateDistanceLabel(String meters){
        String text = "Distance: "+meters+ " meter";
        this.mTxtDistance.setText(text);
    }


}
