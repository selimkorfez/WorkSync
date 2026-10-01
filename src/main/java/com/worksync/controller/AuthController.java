package com.worksync.controller;


import com.worksync.model.User;
import com.worksync.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String email, @RequestParam String password, HttpSession session) {
        User user = userService.authenticate(email, password);
        if (user == null) return ResponseEntity.status(401).body("Invalid email or password");
        session.setAttribute("uid", user.getUid());
        return ResponseEntity.ok(user);
    }

}
