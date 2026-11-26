package com.socsentinel.portal.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import com.socsentinel.portal.service.LoginActivityService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final LoginActivityService loginActivityService;

    public AuditAuthenticationFailureHandler(LoginActivityService loginActivityService) {
        this.loginActivityService = loginActivityService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        loginActivityService.recordFailure(request.getParameter("username"), request.getRemoteAddr());
        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}
