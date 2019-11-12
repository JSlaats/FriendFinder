package com.example.friendfinder.persistence;

import android.location.Location;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.friendfinder.data.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Firestore {
    private FirebaseFirestore db;
    private static String TAG = "FireStore";

    public Firestore() {
        this.db = FirebaseFirestore.getInstance();
    }

    public void loadUser(String phoneNumber){
        DocumentReference docRef = db.collection("users").document(phoneNumber);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        //User user = document.toObject(User.class);
                        Location location = new Location("");
                        location.setLatitude(document.getGeoPoint("lastLocation").getLatitude());
                        location.setLongitude(document.getGeoPoint("lastLocation").getLongitude());

                        User user = new User(
                                document.getString("phonenumber"),
                                document.getString("name"),
                                location,
                                document.getTimestamp("lastOnline").toDate(),
                                document.getBoolean("online")
                        );
                        Log.d(TAG,user.toString());

                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

    }


}
