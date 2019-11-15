package com.example.friendfinder.Fragments;

import androidx.lifecycle.ViewModelProviders;

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
import com.example.friendfinder.data.User;
import com.google.android.gms.maps.model.LatLng;

public class ArrowFragment extends Fragment  {

    private static final String TAG = ArrowFragment.class.getName();
    private ArrowViewModel mViewModel;
    private ImageView mImageViewArrow;
    private TextView mTxtPercentage;
    private TextView mTxtDistance;

    private MapsActivity activity;

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
        mViewModel.initSensors(activity);

        // TODO: Use the ViewModel

    }



    void updateArrow(float myDirection){

        if(activity.getSelectedMarker() != null) {
            LatLng activeMarker = activity.getSelectedMarker().getPosition();
            Location lastLocation = activity.getLocationMapAdapter().getLastLocation();
            LatLng loc = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

            //calculate distance
            float[] result = new float[1];
            Location.distanceBetween(lastLocation.getLatitude(),lastLocation.getLongitude(),activeMarker.latitude,activeMarker.longitude,result);
            Log.v(TAG,"distanceBetween: "+String.format("%.0f",result[0])+" meter");
            updateDistanceLabel(String.format("%.0f",result[0]));

            float angle = mViewModel.angleFromCoordinate(loc, activeMarker);
            float resultAngle = myDirection-angle;
            if(resultAngle < 0)resultAngle += 360;

        //    Log.v(TAG,"Angle: "+angle+" | Direction: "+myDirection+"| result: "+resultAngle+"");

            mTxtPercentage.setText("angle: " + resultAngle + " degrees");
            mImageViewArrow.setRotation(resultAngle);
        }
    }

    void updateDistanceLabel(String meters){

        String text = "Distance: "+meters+ " meter";
        this.mTxtDistance.setText(text);
    }


}
