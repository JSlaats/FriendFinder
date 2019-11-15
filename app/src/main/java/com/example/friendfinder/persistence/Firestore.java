package com.example.friendfinder.persistence;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.friendfinder.MapsActivity;
import com.example.friendfinder.data.User;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Firestore {
    private FirebaseFirestore db;
    private static String TAG = "FireStore";
    private MapsActivity activity;

    public Firestore(MapsActivity activity) {
        this.db = FirebaseFirestore.getInstance();
        this.activity = activity;
    }

    public void loadUser(String phoneNumber){
        final DocumentReference docRef = db.collection("users").document(phoneNumber);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());

                        User user = createUser(document);

                        //load user to activity
                        activity.setUser(user);

                        //load friends
                        List<DocumentReference> friendRefs = (List<DocumentReference>) document.get("friends");
                        if(friendRefs != null) {
                            Log.v(TAG,"Fetching friends");
                            loadFriends(friendRefs);
                        }else{
                            Log.v(TAG,"You have no friends. Sad...");
                        }

                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }


    public void loadFriends(List<DocumentReference> refs){
        for(DocumentReference ref : refs) {
            ref.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {

                            User friend = createUser(document);
                            //add friend to user.
                            activity.addFriend(friend);
                            Log.d(TAG, "Added friend: "+friend.getNickName());

                        } else {
                            Log.d(TAG, "Friend not found");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
            addOnFriendChangeListener(ref);
        }
    }

    private User createUser(DocumentSnapshot document){
        //getting last location
        Location location = null;
        if(document.getGeoPoint("lastLocation") != null) {
            location = new Location("");
            location.setLatitude(document.getGeoPoint("lastLocation").getLatitude());
            location.setLongitude(document.getGeoPoint("lastLocation").getLongitude());
        }
        //get User
        return new User(
                document.getString("phonenumber"),
                document.getString("name"),
                location,
                document.getTimestamp("lastOnline").toDate(),
                document.getBoolean("online")

        );
    }

    private void addOnFriendChangeListener(DocumentReference ref){
        ref.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable final DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    Log.d(TAG, "Current data: " + snapshot.getData());
                    //get friend @ MapsActivity by id
                    //update friend
                    for (User user : activity.getUser().getFriends()) {
                        if (user.getPhoneNumber().equals(snapshot.getString("phonenumber"))) {
                            user.updateUser(createUser(snapshot));

                        }
                    }
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });

    }

    public void register(User user){
        db.collection("users").document(user.getPhoneNumber())
            .set(user)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "User succesfully Registered!");

                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error registering user", e);
                }
            });
    }

    public void saveData(Boolean online, LatLng location){
        User user = activity.getUser();
        if(user == null){Log.v(TAG,"SaveData: User is null.");return;}

        Map<String,Object> data = new HashMap<>();
        data.put("online",online);
        data.put("lastOnline",new Date());
        //only save location if its not null
        if(location != null) {
            data.put("lastLocation", new GeoPoint(location.latitude, location.longitude));
        }
        db.collection("users").document(user.getPhoneNumber())
                .update(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "SaveData: Data succesfully saved in firestore!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "SaveData: Error saving data to firestore", e);
                    }
                });
        ;
    }


}
