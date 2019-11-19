package com.example.friendfinder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.InputType;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.app.ActivityCompat;

import com.example.friendfinder.Fragments.ArrowFragment;
import com.example.friendfinder.Fragments.ArrowViewModel;
import com.example.friendfinder.Fragments.FriendListFragment;
import com.example.friendfinder.data.User;
import com.example.friendfinder.persistence.Firestore;
import com.example.friendfinder.util.Util;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.GeoPoint;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.friendfinder.util.Util.GeoPointToLatLng;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, FriendListFragment.OnListFragmentInteractionListener, PopupMenu.OnMenuItemClickListener {

    private static final String TAG = MainActivity.class.getName();

    private GoogleMap mMap;
    private SupportMapFragment mapFragment;
    private ArrowFragment arrowFragment;
    private LocationMapAdapter locationMapAdapter;
    private PropertyChangeListener listener;

    private User user;
    private Marker selectedMarker;
    private Marker tempMidwayPointMarker;
    private Map<String,Marker> markers;
    private Map<String,Marker> meetupPoints;
    private BitmapDescriptor iconColorOnline;
    private BitmapDescriptor iconColorOffline;
    private BitmapDescriptor iconColorSelected;
    private BitmapDescriptor iconColorMeetup;
    private Firestore firestore;
    private boolean isFirstLogin = false;

    private FirebaseUser firebaseUser;

    private static final int RC_SIGN_IN = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if(firebaseUser == null) createSignInIntent();
        else onCreateContinue();

    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        if(menu instanceof MenuBuilder){
            MenuBuilder m = (MenuBuilder) menu;
            m.setOptionalIconsVisible(true);

        }
        return true;
    }

    private void onCreateContinue(){
        setContentView(R.layout.activity_maps);

        arrowFragment = (ArrowFragment) getSupportFragmentManager().findFragmentById(R.id.arrow_fragment);

        iconColorOnline      = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        iconColorOffline     = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        iconColorSelected    = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN);
        iconColorMeetup    = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA);

        markers = new HashMap<>();
        meetupPoints = new HashMap<>();
        this.firestore = new Firestore(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //mapFragment.getView().setVisibility(View.INVISIBLE);

        initFriendChangedListener();

        mapFragment.getView().setVisibility(View.VISIBLE);
        //load user data
        firestore.loadUser(firebaseUser.getUid());
    }

    public void createSignInIntent() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.GoogleBuilder().build(),
                new AuthUI.IdpConfig.AnonymousBuilder().build()
                );

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setAlwaysShowSignInMethodScreen(true)
                        .setIsSmartLockEnabled(false)
                        .build(),
                RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                onCreateContinue();
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
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
            LatLng defaultLocation = new LatLng(51.4577655,5.4820149);
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(defaultLocation).zoom(11.0f).build()));
          //  mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

            this.locationMapAdapter = new LocationMapAdapter(this,mMap);
            this.locationMapAdapter.startLocationUpdates();


            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    try {
                        if (mMap != null) {
                            //restore marker color if marker is a person
                            if (selectedMarker != null) {
                                if (selectedMarker.getTag() != null) {
                                    if (selectedMarker.getTag().getClass() == Boolean.class) {
                                        if(selectedMarker.getTag().equals(true)){
                                            selectedMarker.setIcon(iconColorOnline);
                                        }else{
                                            selectedMarker.setIcon(iconColorOffline);
                                        }
                                    }
                                }
                            }
                            //if marker that you select is a person, change marker color to selected
                            if(marker.getTag() != "meetup"){
                                marker.setIcon(iconColorSelected);
                            }

                            setSelectedMarker(marker);
                            marker.showInfoWindow();
                            return true;
                        }
                        return false;
                    }catch (Exception ex){
                        Log.e(TAG,"onMarkerClick: "+ex);
                        return false;
                    }
                }
            });
            mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
                @Override
                public void onMarkerDragStart(Marker marker) {
                    setSelectedMarker(marker);
                }

                @Override
                public void onMarkerDrag(Marker marker) {
                    getArrowFragment().getmViewModel().setActiveMarker(marker.getPosition());
                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    //share location
                    String ref = Util.getKeyByValue(meetupPoints,marker);
                    GeoPoint loc = Util.LatLngToGeoPoint(marker.getPosition());
                    firestore.updateMeetupPoint(loc,ref);
                    getArrowFragment().getmViewModel().setActiveMarker(marker.getPosition());
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

    public void setFirstLogin(boolean firstLogin) {
        isFirstLogin = firstLogin;
    }

    public void setSelectedMarker(Marker selectedMarker) {
        this.selectedMarker = selectedMarker;

        arrowFragment.setVisibility(true);
        User friend = user.findFriendByName(selectedMarker.getTitle());

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
            arrowFragment.switchMeetUpButtons(false);
        }else{
            arrowFragment.updateFriendName(selectedMarker.getTitle());
            arrowFragment.updateLastOnline("");
            arrowFragment.switchMeetUpButtons(true);

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
        if(isFirstLogin){
            openSetUsernameDialog();
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
    public void syncLocation(Location location){
        try {
            //only sync if friends are online to see the location change.
            if (user.friendsOnline()) {
                firestore.saveLocation(Util.LocationToLatLng(location));
            }
        }catch (Exception ex){
            Log.e(TAG,"syncLocation failed, user probably null");
        }
    }
    @Override
    public void onListFragmentInteraction(User item) {
        Log.v(TAG,"onListFragmentInteraction");
    }

    public void showMeetUpMenu(View v){
        PopupMenu popup = new PopupMenu(this, v);

        // This activity implements OnMenuItemClickListener
        popup.setOnMenuItemClickListener(this);
        popup.inflate(R.menu.meetup);
        popup.show();

    }

    private void setAutomaticMidWayPoint(){
        LatLng midWayPoint = ArrowViewModel.midPoint(Util.LocationToLatLng(locationMapAdapter.getLastLocation()),selectedMarker.getPosition());
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(midWayPoint)
                .title("Meet-up point")
                .icon(iconColorMeetup)
                .draggable(true));

        marker.setTag("meetup");
        tempMidwayPointMarker = marker;
        String friendUID;
        try {
            friendUID = user.findFriendByName(selectedMarker.getTitle()).getUID();
        }catch (Exception ex){
             friendUID = "";
        }

        firestore.addMeetupPoint(Util.LatLngToGeoPoint(midWayPoint),user.getUID(),friendUID);
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(midWayPoint).zoom(16f).build()));
    }

    public void removeTempMarker(){
        if(tempMidwayPointMarker != null) tempMidwayPointMarker.remove();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.meetup_choose:
            case R.id.meetup_automatic:
                setAutomaticMidWayPoint();
                return true;

            default:
                return false;
        }
    }

    public void addMeetupPoint(String ref,GeoPoint location){
        LatLng loc = GeoPointToLatLng(location);
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(loc)
                .title("Meet-up point")
                .icon(iconColorMeetup)
                .draggable(true));
        marker.setTag("meetup");
        meetupPoints.put(ref,marker);
    }

    public void updateMeetupPoint(String ref, GeoPoint location) {
        LatLng loc = GeoPointToLatLng(location);
        Marker marker = meetupPoints.get(ref);
        if(marker != null){
            marker.setPosition(loc);
        }
    }

    public void removeMeetupPoint(String reference) {
        Marker marker = meetupPoints.get(reference);
        if(marker != null){
            marker.remove();
            meetupPoints.remove(reference);
        }
    }

    public void logout(MenuItem item) {
        Log.v(TAG,"Logout clicked");
        if(getLocationMapAdapter().getLastLocation() != null) {
            firestore.saveData(false, Util.LocationToLatLng(getLocationMapAdapter().getLastLocation()));
            user.setOnline(false);
            user.setLastOnline(new Date());
        }
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        // user is now signed out
                        startActivity(new Intent(MainActivity.this, MainActivity.class));
                        finish();
                    }
                });
    }
    public void openSetUsernameDialog(MenuItem item){
        openSetUsernameDialog();
    }

    public void openSetUsernameDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change your nickname");
        builder.setMessage("This is how friends will see you.");

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(50, 0, 50, 0);


        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        input.setText(user.getNickname());
        input.setLayoutParams(lp);

        container.addView(input);
        builder.setView(container);

        // Set up the buttons
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG,"New username shall be: "+input.getText().toString());
                if(input.getText().toString().isEmpty()){
                    Toast.makeText(getApplicationContext(), "Nickname cannot be empty.", Toast.LENGTH_SHORT).show();
                }else{
                    firestore.updateName(input.getText().toString(),user.getUID());
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d(TAG,"clicked cancel");

                dialog.cancel();
            }
        });

        builder.show();

    }

    public void removeMeetUp(View view) {
        String ref = Util.getKeyByValue(meetupPoints,getSelectedMarker());
        if(ref != null) firestore.removeMeetupPoint(ref);
        getArrowFragment().setVisibility(false);
    }

    public void openAddFriendsDialog(MenuItem item) {
        firestore.addAllUsers(user.getUID());
        /*
        ArrayList<String> newFriendsList = new ArrayList<>();
        newFriendsList.add("aQxRm77QNOPwkvgPD40lzt3GJ8D2");
        firestore.addFriends(user.getUID(),newFriendsList);*/
    }

}
