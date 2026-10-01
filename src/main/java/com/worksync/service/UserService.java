package com.worksync.service;

import com.worksync.model.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService {

    private final Map<String, User> usersByUid = new ConcurrentHashMap<>();
    private final Map<String, String> passwordsByEmail = new ConcurrentHashMap<>();

    public UserService() {
        register(new User("admin", "WorkSync Admin", "admin@worksync.local", "ADMIN"), "admin123");
        register(new User("employee", "WorkSync Employee", "employee@worksync.local", "EMPLOYEE"), "employee123");
    }

    public User getByUid(String uid) {
        User byUid = usersByUid.get(uid);
        if (byUid != null) return byUid;
        return usersByUid.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(uid))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No user found with UID or email: " + uid));
    }

    public String getRoleByUid(String uid) {
        return getByUid(uid).getRole();
    }

    public User authenticate(String email, String password) {
        String storedPassword = passwordsByEmail.get(email.toLowerCase());
        if (storedPassword == null || !storedPassword.equals(password)) return null;
        return usersByUid.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    public List<User> getAllUsers() {
        return List.copyOf(usersByUid.values());
    }

    public void saveUser(User user) {
        register(user, passwordsByEmail.getOrDefault(user.getEmail().toLowerCase(), "employee123"));
    }

    private void register(User user, String password) {
        usersByUid.put(user.getUid(), user);
        passwordsByEmail.put(user.getEmail().toLowerCase(), password);
    }
}
