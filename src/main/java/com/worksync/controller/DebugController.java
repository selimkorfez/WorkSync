package com.worksync.controller;

import com.worksync.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
public class DebugController {

    private final UserService userService;

    public DebugController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/debug/role")
    public String debugRole(@RequestParam String uid) throws ExecutionException, InterruptedException {
        String role = userService.getRoleByUid(uid);
        return "Role for UID [" + uid + "]: " + role;
    }
}
