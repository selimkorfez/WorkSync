package com.worksync.service;


import com.google.cloud.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.worksync.model.User;
import org.springframework.stereotype.Service;
import com.google.firebase.cloud.FirestoreClient;
import com.worksync.model.Task;
import com.google.api.core.ApiFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.UUID;


@Service
public class FirebaseService {

    public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }
    public void createTask(Task task) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        // Generate unique task ID
        String taskId = UUID.randomUUID().toString();
        task.setId(taskId);

        // Save task to Firestore
        db.collection("tasks").document(taskId).set(task).get();
    }
    public List<Task> getAllTasks() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection("tasks").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Task> tasks = new ArrayList<>();
        for (QueryDocumentSnapshot doc : documents) {
            Task task = doc.toObject(Task.class);
            tasks.add(task);
        }

        return tasks;
    }
    public List<Task> getTasksByUserAndStatus(String uid, String status, String sort) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Query query = db.collection("tasks").whereEqualTo("assignedToUid", uid);


        if (status != null && !status.isEmpty()) {
            query = query.whereEqualTo("status", status);
        }

        if (sort != null && !sort.isEmpty()) {
            query = query.orderBy(sort); // sort safely
        }

        ApiFuture<QuerySnapshot> future = query.get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        List<Task> tasks = new ArrayList<>();
        for (QueryDocumentSnapshot doc : documents) {
            tasks.add(doc.toObject(Task.class));
        }

        return tasks;
    }
    public User getByUid(String uid) {
        Firestore db = FirestoreClient.getFirestore();

        try {
            DocumentSnapshot doc = db.collection("users").document(uid).get().get();
            if (doc.exists()) {
                return doc.toObject(User.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public List<Task> getTasksByUser(String uid) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection("tasks")
                .whereEqualTo("assignedToUid", uid)
                .get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        List<Task> tasks = new ArrayList<>();
        for (QueryDocumentSnapshot doc : documents) {
            tasks.add(doc.toObject(Task.class));
        }
        return tasks;
    }
    public User getUserByUid(String uid) throws ExecutionException, InterruptedException {
        if (uid == null || uid.isBlank()) return null;

        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot snapshot = db.collection("users").document(uid).get().get();
        return snapshot.exists() ? snapshot.toObject(User.class) : null;
    }

}




