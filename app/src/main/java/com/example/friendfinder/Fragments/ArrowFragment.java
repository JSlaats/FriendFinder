package com.example.friendfinder.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.friendfinder.MapsActivity;
import com.example.friendfinder.R;

public class ArrowFragment extends Fragment {

    private static final String TAG = ArrowFragment.class.getName();
    private ArrowViewModel mViewModel;
    private ImageView mImageViewArrow;
    private TextView mTxtFriendName;
    private TextView mTxtLastOnline;
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
        mTxtFriendName = getView().findViewById(R.id.txtFriendName);
        mTxtLastOnline = getView().findViewById(R.id.txtLastOnline);
        mTxtDistance = getView().findViewById(R.id.txtDistance);
        //TODO: hide button from the xml if meetpoint is selected
        //TODO: Display with whom i share the marker
        this.activity = (MapsActivity) getActivity();
        activity.setArrowFragment(this);

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

    public ArrowViewModel getmViewModel() {
        return mViewModel;
    }

    void updateArrow(final float rotation){
         mImageViewArrow.setRotation(rotation);
    }

    void updateDistanceLabel(final String meters){
        String text = "Distance: "+meters+ " meter";
        mTxtDistance.setText(text);
    }

    public void updateFriendName(String friendName){
        mTxtFriendName.setText(friendName);
    }

    public void updateLastOnline(String lastOnline){
        mTxtLastOnline.setText(lastOnline);
    }

    public void setVisibility(boolean show){
        if(show) getView().setVisibility(View.VISIBLE);
        else getView().setVisibility(View.INVISIBLE);
    }

}
