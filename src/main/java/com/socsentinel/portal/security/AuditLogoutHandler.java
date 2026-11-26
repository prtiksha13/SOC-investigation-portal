package com.socsentinel.portal.security;

import com.socsentinel.portal.service.LoginActivityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
public class AuditLogoutHandler implements LogoutHandler {
    private final LoginActivityService loginActivityService;

    public AuditLogoutHandler(LoginActivityService loginActivityService) {
        this.loginActivityService = loginActivityService;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null && request.getSession(false) != null) {
            loginActivityService.recordLogout(request.getSession(false).getId(), LocalDateTime.now());
        }
    }
}
