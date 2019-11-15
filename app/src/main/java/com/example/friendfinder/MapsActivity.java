package com.example.friendfinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;

import com.example.friendfinder.Fragments.ArrowFragment;
import com.example.friendfinder.Fragments.FriendListFragment;
import com.example.friendfinder.data.User;
import com.example.friendfinder.persistence.Firestore;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, FriendListFragment.OnListFragmentInteractionListener {

    private static final String TAG = MapsActivity.class.getName();
    private GoogleMap mMap;
    private ArrowFragment arrowFragment;
    private LocationMapAdapter locationMapAdapter;
    private Marker selectedMarker;
    private Map<String,Marker> markers;
    private User user;
    public PropertyChangeListener listener;


    private String MY_PHONE_NUMBER = "31611507210";

    BitmapDescriptor iconColorOnline  ;
    BitmapDescriptor iconColorOffline ;
    BitmapDescriptor iconColorSelected;


    public void setArrowFragment(ArrowFragment arrowFragment) {
        this.arrowFragment = arrowFragment;

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        arrowFragment = (ArrowFragment) getSupportFragmentManager().findFragmentById(R.id.arrow_fragment);
        initFriendChangedListener();
        iconColorOnline      = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        iconColorOffline     = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        iconColorSelected    = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
        markers = new HashMap<>();
    }



    private void addMarkers(){
        LatLng loc = new LatLng(51.457704, 5.482136);
        LatLng loc2 = new LatLng(51.503364, 5.370738);
        LatLng loc3 = new LatLng(51.458483, 5.542588);

        mMap.addMarker(new MarkerOptions().position(loc2).title("Friend"));
        mMap.addMarker(new MarkerOptions().position(loc3).title("another friend"));

        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(loc).zoom(12.0f).build()));
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        boolean permissionAccessCoarseLocationApproved = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (permissionAccessCoarseLocationApproved) {
            // App has permission to access location in the foreground. Start your
            // foreground service that has a foreground service type of "location".
            mMap.setMyLocationEnabled(true);
            this.locationMapAdapter = new LocationMapAdapter(this,mMap);
            locationMapAdapter.startLocationUpdates();


            new Firestore(this).loadUser(MY_PHONE_NUMBER);


            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if(mMap != null) {

                        if(selectedMarker == marker){
                            selectedMarker = null;
                            return true;
                        }
                        if(selectedMarker != null){
                            selectedMarker.setIcon(iconColorOffline);
                            if(selectedMarker.getTag() != null){
                                 if((boolean)selectedMarker.getTag()){
                                    selectedMarker.setIcon(iconColorOnline);
                                }
                            }
                        }
                        marker.setIcon(iconColorSelected);
                        marker.showInfoWindow();
                        selectedMarker = marker;

                    //    arrowFragment.updateArrow();
                        return true;
                    }return false;
                }
            });
        } else {
            // Make a request for foreground-only location access.
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
    }


    public Marker getSelectedMarker() {
        return selectedMarker;
    }

    public ArrowFragment getArrowFragment() {
        return arrowFragment;
    }

    public LocationMapAdapter getLocationMapAdapter() {
        return locationMapAdapter;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    public void addFriend(User friend){
        this.user.addFriend(friend);
        friend.changes.addPropertyChangeListener(listener);

        String onlineString;
        BitmapDescriptor iconColor;

        if(friend.isOnline()){
            onlineString = "Online";
            iconColor = iconColorOnline;
        }
        else{
            String lastSeenString = DateUtils.getRelativeTimeSpanString(friend.getLastOnline().getTime()).toString();

            onlineString = "Last Seen: "+lastSeenString;
            iconColor = iconColorOffline;
        }

        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(friend.getLastLocation())
                .title(friend.getNickName())
                .snippet(onlineString)
                .icon(iconColor));
        marker.setTag(friend.isOnline());
        markers.put(friend.getPhoneNumber(),marker);
    }

    private void initFriendChangedListener(){
        listener= new PropertyChangeListener() { //This is how we define the listener and tell it what to do when it hears something change
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                User friend = (User)event.getSource();
                Marker marker = markers.get(friend.getPhoneNumber());

                switch(event.getPropertyName()){
                    case "lastLocation":
                        //update marker position
                        assert marker != null;
                        marker.setPosition(friend.getLastLocation());
                        break;
                    case "lastOnline":
                    case "online":
                        //if online: hide lastonline
                        if(friend.isOnline()) {
                            marker.setIcon(iconColorOnline);
                            marker.setSnippet("Online");
                        }else{
                            marker.setIcon(iconColorOffline);
                            String lastSeenString = DateUtils.getRelativeTimeSpanString(friend.getLastOnline().getTime()).toString();
                            marker.setSnippet("Last Seen: "+lastSeenString);
                        }
                        break;
                    default:break;
                }
                Log.v(TAG,event.toString());
            }
        };
    }
    @Override
    public void onListFragmentInteraction(User item) {
        Log.v(TAG,"onListFragmentInteraction");
    }
}
