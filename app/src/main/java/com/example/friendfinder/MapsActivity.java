package com.example.friendfinder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ArrowFragment arrowFragment;
    private LocationMapAdapter locationMapAdapter;
    private Marker selectedMarker;

    public void setArrowFragment(ArrowFragment arrowFragment) {
        this.arrowFragment = arrowFragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        arrowFragment = (ArrowFragment) getSupportFragmentManager().findFragmentById(R.id.arrow_fragment);

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
            addMarkers();

            mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker) {
                    if(mMap != null) {

                        if(selectedMarker != null)selectedMarker.setIcon(null);
                        marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
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
}
