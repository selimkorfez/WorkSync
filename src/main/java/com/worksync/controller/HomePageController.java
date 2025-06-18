package com.worksync.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomePageController {

    @GetMapping("/login")
    public String login() {
        return "login"; // login.html
    }

    @GetMapping("/")
    public String homepage() {
        return "homepage"; // homepage.html
    }
}
