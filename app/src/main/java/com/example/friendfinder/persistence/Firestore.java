package com.example.friendfinder.persistence;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;

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

    public void loadUser(String UID){
        final DocumentReference docRef = db.collection("users").document(UID);
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
                        List<String> friendRefs = (List<String>) document.get("friends");
                        if(friendRefs != null) {
                            Log.v(TAG,"Fetching friends");
                            loadFriends(friendRefs);
                        }else{
                            Log.v(TAG,"You have no friends. Sad...");
                        }

                    } else {
                        Log.d(TAG, "No such document");
                        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();
                        //register user
                        if(fbu != null) {
                            register(new User(fbu.getUid(), fbu.getDisplayName()));
                        }
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });
    }


    public void loadFriends(List<String> friendUIDs){
        for(String friendUID : friendUIDs) {
            db.collection("users").document(friendUID)
                    .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {

                            User friend = createUser(document);
                            //add friend to user.
                            activity.addFriend(friend);
                            Log.d(TAG, "Added friend: "+friend.getNickname());

                        } else {
                            Log.d(TAG, "Friend not found");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
            addOnFriendChangeListener(friendUID);
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
                document.getString("UID"),
                document.getString("nickname"),
                location,
                document.getTimestamp("lastOnline").toDate(),
                document.getBoolean("online")

        );
    }

    private void addOnFriendChangeListener(String friendUID){
        db.collection("users").document(friendUID)
        .addSnapshotListener(new EventListener<DocumentSnapshot>() {
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
                        if (user.getUID().equals(snapshot.getString("UID"))) {
                            user.updateUser(createUser(snapshot));

                        }
                    }
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });

    }

    public void register(final User user){
        db.collection("users").document(user.getUID())
            .set(user.getUser())
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "User succesfully Registered!");
                    loadUser(user.getUID());
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
        db.collection("users").document(user.getUID())
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
    public void saveLocation(LatLng location){
        User user = activity.getUser();
        if(user == null){Log.v(TAG,"saveLocation: User is null.");return;}

        Map<String,Object> data = new HashMap<>();
        //only save location if its not null
        if(location != null) {
            data.put("lastLocation", new GeoPoint(location.latitude, location.longitude));
        }
        db.collection("users").document(user.getUID())
                .update(data)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "saveLocation: Location succesfully saved in firestore!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "saveLocation: Error saving Location to firestore", e);
                    }
                });
        ;
    }

}
