package com.socsentinel.portal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.socsentinel.portal.service.UserService;

@Controller
public class ForgotPasswordController {
    private final UserService userService;

    public ForgotPasswordController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/forgot-password")
    public String forgotPassword() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password/verify")
    public String verifyUsername(@RequestParam String username, Model model) {
        String normalizedUsername = username.trim();
        if (normalizedUsername.isEmpty() || !userService.userExists(normalizedUsername)) {
            model.addAttribute("error", "Username not found.");
            model.addAttribute("username", normalizedUsername);
            return "forgot-password";
        }

        model.addAttribute("username", normalizedUsername);
        model.addAttribute("verified", true);
        return "forgot-password";
    }

    @PostMapping("/forgot-password/reset")
    public String resetPassword(@RequestParam String username,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model) {
        if (newPassword.isBlank()) {
            return resetError(model, username, "New password is required.");
        }
        if (!newPassword.equals(confirmPassword)) {
            return resetError(model, username, "Passwords do not match.");
        }
        if (!userService.userExists(username)) {
            return resetError(model, username, "Username not found.");
        }

        userService.resetPassword(username, newPassword);
        return "redirect:/login?reset=success";
    }

    private String resetError(Model model, String username, String error) {
        model.addAttribute("username", username);
        model.addAttribute("verified", true);
        model.addAttribute("error", error);
        return "forgot-password";
    }
}
