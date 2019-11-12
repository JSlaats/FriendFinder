package com.example.friendfinder.data;

import android.location.Location;

import com.google.firebase.firestore.GeoPoint;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User {
    private String phoneNumber;
    private String nickname;
    private Location lastLocation;
    private Date lastOnline;
    private boolean online;
    private ArrayList<User> friends;

    public User(String phoneNumber, String name, GeoPoint lastLocation, Date lastOnline, boolean online, ArrayList<User> friends) {
        this.phoneNumber = phoneNumber;
        this.nickname = name;
        this.lastOnline = lastOnline;
        this.online = online;
        this.friends = friends;
        Location location = new Location("");
        location.setLatitude(lastLocation.getLatitude());
        location.setLongitude(lastLocation.getLongitude());
        this.lastLocation = location;
    }

    public User() {
    }

    public User(String phoneNumber, String name, Location lastLocation, Date lastOnline, boolean online) {
        this.phoneNumber = phoneNumber;
        this.nickname = name;
        this.lastLocation = lastLocation;
        this.lastOnline = lastOnline;
        this.online = online;
    }


    public User(String phoneNumber, String name) {
        this.phoneNumber = phoneNumber;
        this.nickname = name;
        this.lastLocation = null;
        this.lastOnline = null;
        this.online = false;
    }

    public String getNickName() {
        return nickname;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public Date getLastOnline() {
        return lastOnline;
    }

    public boolean isOnline() {
        return online;
    }

//    public void setLastLocation(Location lastLocation) {
//        this.lastLocation = lastLocation;
//    }

    public void setLastLocation(GeoPoint lastLocation) {
        Location location = new Location("");
        location.setLatitude(lastLocation.getLatitude());
        location.setLongitude(lastLocation.getLongitude());
        this.lastLocation = location;

    }

    public void setLastOnline(Date lastOnline) {
        this.lastOnline = lastOnline;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public ArrayList<User> getFriends() {
        return friends;
    }

    public void setFriends(ArrayList<User> friends) {
        this.friends = friends;
    }

}
