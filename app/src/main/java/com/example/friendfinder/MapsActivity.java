package com.example.friendfinder;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.friendfinder.Fragments.ArrowFragment;
import com.example.friendfinder.Fragments.FriendListFragment;
import com.example.friendfinder.data.User;
import com.example.friendfinder.persistence.Firestore;
import com.example.friendfinder.util.Util;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, FriendListFragment.OnListFragmentInteractionListener {

    private static final String TAG = MapsActivity.class.getName();

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private ArrowFragment arrowFragment;
    private LocationMapAdapter locationMapAdapter;
    private PropertyChangeListener listener;

    private User user;
    private Marker selectedMarker;
    private Map<String,Marker> markers;
    private BitmapDescriptor iconColorOnline;
    private BitmapDescriptor iconColorOffline;
    private BitmapDescriptor iconColorSelected;
    private Firestore firestore;
    private FirebaseUser firebaseUser;

    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        arrowFragment = (ArrowFragment) getSupportFragmentManager().findFragmentById(R.id.arrow_fragment);

        iconColorOnline      = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        iconColorOffline     = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        iconColorSelected    = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
        markers = new HashMap<>();
        this.firestore = new Firestore(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapFragment.getView().setVisibility(View.INVISIBLE);

        initFriendChangedListener();

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if(user == null) createSignInIntent();
        else onCreateContinue();

    }
    private void onCreateContinue(){
        mapFragment.getView().setVisibility(View.VISIBLE);
        //load user data
        firestore.loadUser(firebaseUser.getUid());
    }

    public void createSignInIntent() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build()
        );

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN);
        // [END auth_fui_create_intent]
    }
    // [START auth_fui_result]

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                onCreateContinue();
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }
    // [END auth_fui_result]
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


            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if(mMap != null) {
                        if(selectedMarker == marker){
                           // setSelectedMarker(null);
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
                        setSelectedMarker(marker);
                        marker.showInfoWindow();

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
//region lifecycle
    @Override
    protected void onResume() {
        super.onResume();
        try{
            firestore.saveData(true, Util.LocationToLatLng(getLocationMapAdapter().getLastLocation()));
            user.setOnline(true);
            user.setLastOnline(new Date());
            user.setLastLocation(getLocationMapAdapter().getLastLocation());
        }catch(Exception ex){
            Log.e(TAG,"OnResume: Saving failed: LastLocation not initiated yet. "+ex);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
        firestore.saveData(false, Util.LocationToLatLng(getLocationMapAdapter().getLastLocation()));
        user.setOnline(false);
        user.setLastOnline(new Date());
        }catch(Exception ex){
            Log.e(TAG,"OnPause: Saving failed. "+ex);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        try{
        firestore.saveData(false, Util.LocationToLatLng(getLocationMapAdapter().getLastLocation()));
        user.setOnline(false);
        user.setLastOnline(new Date());
        }catch(Exception ex){
            Log.e(TAG,"OnStop: Saving failed. "+ex);
        }
    }

//endregion

    //region Getters and Setters
    public Marker getSelectedMarker() {
        return selectedMarker;
    }

    public void setSelectedMarker(Marker selectedMarker) {
        this.selectedMarker = selectedMarker;

        arrowFragment.setVisibility(true);

        User friend = user.findFriendByName(selectedMarker.getTitle());
       // Log.e(TAG,selectedMarker.getTitle()+"  |  "+friend.getNickname());

        if(friend != null){
            arrowFragment.updateFriendName(friend.getNickname());
            String isOnlineString;
            if(friend.isOnline()) {
                isOnlineString = "Online";
            }else{
                String lastSeenString = DateUtils.getRelativeTimeSpanString(friend.getLastOnline().getTime()).toString();
                isOnlineString = "Last Seen: "+lastSeenString;
            }
            arrowFragment.updateLastOnline(isOnlineString);
        }else{
            arrowFragment.updateFriendName(selectedMarker.getTitle());
            arrowFragment.updateLastOnline("");
        }
        getArrowFragment().getmViewModel().setActiveMarker(selectedMarker.getPosition());

    }

    public void setArrowFragment(ArrowFragment arrowFragment) {
        this.arrowFragment = arrowFragment;
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
        user.setOnline(true);
        user.setLastOnline(new Date());
        this.user = user;
        try {
            firestore.saveData(true, Util.LocationToLatLng(getLocationMapAdapter().getLastLocation()));
        }catch(Exception ex){
            Log.e(TAG,"setUser: Saving failed. "+ex);
         }
    }
    //endregion

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
                .title(friend.getNickname())
                .snippet(onlineString)
                .icon(iconColor));
        marker.setTag(friend.isOnline());
        markers.put(friend.getUID(),marker);
    }

    private void initFriendChangedListener(){
        listener= new PropertyChangeListener() { //This is how we define the listener and tell it what to do when it hears something change
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                User friend = (User)event.getSource();
                Marker marker = markers.get(friend.getUID());

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
