package com.blogspot.tndev1403.antitheft.Modal;

import com.blogspot.tndev1403.antitheft.Stored.Config.Config;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Firebase {
    public static FirebaseDatabase database = FirebaseDatabase.getInstance();
    public static DatabaseReference databaseReference = database.getReference(Config.REFERENCE_STRING);
}
