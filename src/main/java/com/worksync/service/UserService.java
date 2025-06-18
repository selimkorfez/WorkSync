package com.worksync.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.worksync.model.User;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {

    public User getByUid(String uid) {
        System.out.println("📄 Looking for Firestore user with document ID: " + uid);

        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentSnapshot doc = db.collection("users").document(uid).get().get();

            if (doc.exists()) {
                return doc.toObject(User.class);
            }

            // search by email
            CollectionReference users = db.collection("users");
            QuerySnapshot query = users.whereEqualTo("email", uid).get().get();
            if (!query.isEmpty()) {
                return query.getDocuments().get(0).toObject(User.class);
            }

            throw new IllegalArgumentException("No user found with UID or email: " + uid);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("Failed to get user with UID/email: " + uid);
        }
    }




    public String getRoleByUid(String uid) throws ExecutionException, InterruptedException {
        System.out.println("👤 getRoleByUid with: " + uid);

        User user = getByUid(uid);
        return user != null ? user.getRole() : "EMPLOYEE"; // fallback default
    }

    public void saveUser(User user) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        db.collection("users").document(user.getUid()).set(user).get();
    }
    public List<User> getAllUsers() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection("users").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<User> users = new ArrayList<>();
        for (QueryDocumentSnapshot doc : documents) {
            users.add(doc.toObject(User.class));
        }

        return users;
    }

}
