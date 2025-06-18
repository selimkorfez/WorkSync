package com.worksync.controller;


import com.worksync.service.FirebaseService;
import com.worksync.service.UserService;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final FirebaseService firebaseService;
    private final UserService userService;

    public AuthController(FirebaseService firebaseService, UserService userService) {
        this.firebaseService = firebaseService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestHeader("Authorization") String authHeader, HttpServletRequest request) {
        try {
            String token = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = firebaseService.verifyToken(token);

            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = decodedToken.getName() != null ? decodedToken.getName() : "Unnamed User";


            // Save UID to session
            request.getSession().setAttribute("uid", uid);

            return ResponseEntity.ok("Login success");

        } catch (FirebaseAuthException e) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Login failed");
        }
    }

}
