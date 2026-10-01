package com.worksync.controller;

import com.worksync.model.User;
import com.worksync.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.ui.Model;

@Controller
public class HomePageController {

    private final UserService userService;

    public HomePageController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "login"; // login.html
    }

    @GetMapping("/")
    public String homepage(HttpSession session, Model model) {
        String uid = (String) session.getAttribute("uid");
        if (uid == null) return "redirect:/login";
        User user = userService.getByUid(uid);
        model.addAttribute("uid", uid);
        model.addAttribute("email", user.getEmail());
        model.addAttribute("urgentTasks", java.util.List.of());
        model.addAttribute("dueTasks", java.util.List.of());
        return "homepage";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session,
                        RedirectAttributes redirectAttributes) {
        User user = userService.authenticate(email, password);
        if (user == null) {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password.");
            return "redirect:/login";
        }
        session.setAttribute("uid", user.getUid());
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
