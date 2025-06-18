package com.worksync.controller;

import com.worksync.model.User;
import com.worksync.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/role")
    public ResponseEntity<String> getRole(@RequestParam("uid") String uid) throws ExecutionException, InterruptedException {
        String role = userService.getRoleByUid(uid);
        return ResponseEntity.ok(role);
    }

    @GetMapping("/all")
    @ResponseBody
    public List<User> getAllUsers() throws ExecutionException, InterruptedException {
        return userService.getAllUsers();
    }
    @GetMapping("/non-admin")
    @ResponseBody
    public List<User> getNonAdminUsers() throws ExecutionException, InterruptedException {
        List<User> allUsers = userService.getAllUsers();
        return allUsers.stream()
                .filter(user -> !"ADMIN".equalsIgnoreCase(user.getRole()))
                .collect(Collectors.toList());
    }


}


