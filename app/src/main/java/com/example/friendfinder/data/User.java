package com.example.friendfinder.data;

import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;

import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class User {
    private static final String TAG = User.class.getName();
    private String UID;
    private String nickname;
    private Location lastLocation;
    private Date lastOnline;
    private boolean online;
    private ArrayList<User> friends;

    public PropertyChangeSupport changes = new PropertyChangeSupport(this);

    public User(String UID, String name, GeoPoint lastLocation, Date lastOnline, boolean online, ArrayList<User> friends) {
        this.UID = UID;
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

    public Map<String,Object> getUser(){
        Map<String, Object> data = new HashMap<>();
        data.put("UID", getUID());
        data.put("nickname", getNickname());
        data.put("lastLocation", getLastLocation());
        data.put("lastOnline", getLastOnline());
        data.put("online", isOnline());
        data.put("friends", getFriendsRef());
        return data;
    }

    public User(String UID, String name, Location lastLocation, Date lastOnline, boolean online) {
        this.UID = UID;
        this.nickname = name;
        this.lastLocation = lastLocation;
        this.lastOnline = lastOnline;
        this.online = online;
        this.friends = new ArrayList<>();
    }


    public User(String UID, String name) {
        this.UID = UID;
        this.nickname = name;
        this.lastLocation = null;
        this.lastOnline = new Date();
        this.online = false;
        this.friends = new ArrayList<>();
    }

    public void updateUser(User user){
        if(!this.nickname.equals(user.nickname))
        setNickname(user.nickname);

        if(this.lastLocation.getLatitude() != user.lastLocation.getLatitude() || this.lastLocation.getLongitude() != user.lastLocation.getLongitude())
        setLastLocation(user.lastLocation);

        if(this.lastOnline != user.lastOnline)
        setLastOnline(user.lastOnline);

        if(this.online != user.online)
        setOnline(user.online);

        Log.v(TAG,"Updated user: "+nickname);
    }

    public String getNickname() {
        if(nickname == null)return "anonymous";
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

//    public Location getLastLocation() {
//        return lastLocation;
//    }
    public LatLng getLastLocation(){
        if(this.lastLocation == null)return null;
        return new LatLng(this.lastLocation.getLatitude(),this.lastLocation.getLongitude());
    }

    public Date getLastOnline() {
        return lastOnline;
    }

    public boolean isOnline() {
        return online;
    }

    public String getUID() {
        return UID;
    }

//    public void setLastLocation(Location lastLocation) {
//        this.lastLocation = lastLocation;
//    }

//    public void setLastLocation(GeoPoint lastLocation) {
//        Location location = new Location("");
//        location.setLatitude(lastLocation.getLatitude());
//        location.setLongitude(lastLocation.getLongitude());
//        this.lastLocation = location;
//    }

    public void setLastLocation(Location location){
        this.changes.firePropertyChange("lastLocation",this.lastLocation,location);
        this.lastLocation = location;
    }

    public void setLastOnline(Date lastOnline) {
        this.changes.firePropertyChange("lastOnline",this.lastLocation,lastOnline);

        this.lastOnline = lastOnline;
    }

    public void setOnline(boolean online) {
        this.changes.firePropertyChange("online",this.online,online);

        this.online = online;

    }

    public ArrayList<User> getFriends() {
        return friends;
    }

    public ArrayList<String> getFriendsRef(){
        ArrayList<String> friendArr = new ArrayList<>();
        for(User friend : getFriends()){
            friendArr.add(friend.getUID());
        }
        return friendArr;
    }

    public void setFriends(ArrayList<User> friends) {
        this.friends = friends;
    }

    public void addFriend(User user) {
        friends.add(user);
    }
    public User findFriendByName(String nickname){
        for(User friend : getFriends()){
            if(friend.getNickname().equals(nickname))return friend;
        }
        return null;
    }
    public Boolean friendsOnline(){
        for(User friend : getFriends()){
            if(friend.isOnline())return true;
        }
        return false;
    }
}
