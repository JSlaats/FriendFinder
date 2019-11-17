package com.example.friendfinder.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.friendfinder.MainActivity;
import com.example.friendfinder.data.User;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Firestore {
    private FirebaseFirestore db;
    private static String TAG = "FireStore";
    private MainActivity activity;

    public Firestore(MainActivity activity) {
        this.db = FirebaseFirestore.getInstance();
        this.activity = activity;
    }

    public void addMeetupPoint(GeoPoint midWayPoint,String myUID, String friendUID) {
        ArrayList<String> userList = new ArrayList<>();
        userList.add(myUID);
        if(!friendUID.isEmpty())
        userList.add(friendUID);

        Map<String,Object> markerMap = new HashMap<>();
        markerMap.put("location",midWayPoint);
        markerMap.put("users",userList);

        db.collection("meetup").document()
            .set(markerMap)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "MeetupPoint saved!");
                    activity.removeTempMarker();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error saving MeetupPoint", e);
                }
            });
    }

    public void updateMeetupPoint(GeoPoint location,String ref) {
        db.collection("meetup").document(ref).update("location",location)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "MeetupPoint updated!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error updating MeetupPoint", e);
                }
            });
    }

    public void removeMeetupPoint(String ref) {
        //TODO:: Implement this. Change meetpoint button to remove when meetpoint is selected and run this fucntion.


        db.collection("meetup").document(ref).delete()
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.d(TAG, "MeetupPoint Deleted!");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error deleting MeetupPoint", e);
                }
            });
    }

    private void addMeetupPointListener(String UID){
        db.collection("meetup").whereArrayContains("users",UID).addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException e) {
                //meetup point added/changed
                for (DocumentChange dc : value.getDocumentChanges()) {
                    String reference = dc.getDocument().getReference().getId();
                    Log.v(TAG,reference);
                    switch (dc.getType()) {
                        case ADDED:
                            Log.d(TAG, "ADDED meet-up point: " + dc.getDocument().getData());
                            activity.addMeetupPoint(reference,dc.getDocument().getGeoPoint("location"));
                            break;
                        case MODIFIED:
                            Log.d(TAG, "MODIFIED meet-up point: " + dc.getDocument().getData());
                            activity.updateMeetupPoint(reference,dc.getDocument().getGeoPoint("location"));
                            break;
                        case REMOVED:
                            Log.d(TAG, "REMOVED meet-up point: " + dc.getDocument().getData());
                            activity.removeMeetupPoint(reference);
                            break;
                    }
                }

            }
        });
    }
    private boolean triedToRestoreUser = false;

    public void loadUser(final String UID){
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
                            addMeetupPointListener(UID);
                        }else{
                            Log.v(TAG,"You have no friends. Sad...");
                        }

                    } else {
                        Log.d(TAG, "No such document");
                        FirebaseUser fbu = FirebaseAuth.getInstance().getCurrentUser();

                        //register user
                        if(fbu != null) {
                            if(fbu.isAnonymous()){
                                SharedPreferences sharedPref = activity.getApplicationContext().getSharedPreferences("login", Context.MODE_PRIVATE);
                                //if you registered as anonymous before, load that user
                                if(sharedPref.getString("UID",null) != null && !triedToRestoreUser){
                                    Log.d(TAG,"Anoymous user already existed, loading user");
                                    triedToRestoreUser = true;
                                    loadUser(sharedPref.getString("UID",null));

                                }else {
                                    //if not registered as anonymous before, register and save that UID
                                    Log.d(TAG,"Anoymous user not found. Registering.");

                                    register(new User(fbu.getUid(), "anon-"+new Date().getTime()));
                                    sharedPref.edit().putString("UID",fbu.getUid()).apply();
                                }
                            }else{
                                //Logged in with google, making new account
                                Log.d(TAG,"Logged in with google, making new account.");
                                register(new User(fbu.getUid(), fbu.getDisplayName()));
                            }
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
                    //get friend @ MainActivity by id
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
        activity.setFirstLogin(true);
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

    }

    public void updateName(final String nickname, String UID) {

        db.collection("users").document(UID)
                .update("nickname",nickname)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "updateName: nickname succesfully saved in firestore!");
                        activity.getUser().setNickname(nickname);
                        Toast.makeText(activity, "Nickname changed to: "+nickname, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(activity, "Error saving nickname", Toast.LENGTH_SHORT).show();
                        Log.w(TAG, "updateName: Error saving nickname", e);
                    }
                });
    }
}
