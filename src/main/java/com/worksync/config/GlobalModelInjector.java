package com.worksync.config;

import com.worksync.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class GlobalModelInjector {

    @Autowired
    private UserService userService;

    @ModelAttribute
    public void addUserAttributes(Model model, HttpServletRequest request) {
        String uid = request.getParameter("uid");
        if (uid != null && !uid.isBlank()) {
            model.addAttribute("uid", uid);
            try {
                String role = userService.getRoleByUid(uid);
                model.addAttribute("userRole", role);
            } catch (Exception e) {
                model.addAttribute("userRole", "EMPLOYEE"); // fallback
            }
        }
    }
}
