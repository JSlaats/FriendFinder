package com.example.friendfinder;

import androidx.lifecycle.ViewModelProviders;

import android.graphics.Matrix;
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

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class ArrowFragment extends Fragment {

    private ArrowViewModel mViewModel;
    private ImageView mImageViewArrow;
    private TextView mTxtPercentage;
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

        this.activity = (MapsActivity) getActivity();
        activity.setArrowFragment(this);
        // TODO: Use the ViewModel

    }



    public void updateArrow(LatLng activeMarker){
        Location lastLocation = activity.getLocationMapAdapter().getLastLocation();
        LatLng loc = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());

        float angle = mViewModel.angleFromCoordinate(loc,activeMarker);
        mTxtPercentage.setText("angle: "+angle+" degrees");
        mImageViewArrow.setRotation(angle);
    }



}
