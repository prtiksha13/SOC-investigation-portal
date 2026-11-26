package com.socsentinel.portal.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.socsentinel.portal.service.LoginActivityService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuditAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final LoginActivityService loginActivityService;

    public AuditAuthenticationSuccessHandler(LoginActivityService loginActivityService) {
        this.loginActivityService = loginActivityService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        loginActivityService.recordSuccess(
                authentication.getName(), request.getRemoteAddr(), request.getSession().getId());
        response.sendRedirect(request.getContextPath() + "/");
    }
}
