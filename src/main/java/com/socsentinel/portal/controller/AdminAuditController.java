package com.socsentinel.portal.controller;

import com.socsentinel.portal.model.LoginActivity;
import com.socsentinel.portal.service.LoginActivityService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminAuditController {
    private final LoginActivityService loginActivityService;

    public AdminAuditController(LoginActivityService loginActivityService) {
        this.loginActivityService = loginActivityService;
    }

    @GetMapping("/login-activity")
    public AuditResponse loginActivity() {
        List<ActivityResponse> activities = loginActivityService.findAll().stream()
                .map(ActivityResponse::from)
                .toList();
        return new AuditResponse(activities, loginActivityService.countsByUsername());
    }

    public record AuditResponse(List<ActivityResponse> activities,
                                Map<String, LoginActivityService.LoginCount> counts) {
    }

    public record ActivityResponse(Long id, String username, String role, LocalDateTime loginAt,
                                   LocalDateTime logoutAt, String status, String ipAddress,
                                   Long sessionDurationSeconds) {
        static ActivityResponse from(LoginActivity activity) {
            return new ActivityResponse(
                    activity.getId(), activity.getUsername(), activity.getRole(), activity.getLoginAt(),
                    activity.getLogoutAt(), activity.getStatus().name(), activity.getIpAddress(),
                    activity.getSessionDurationSeconds());
        }
    }
}
