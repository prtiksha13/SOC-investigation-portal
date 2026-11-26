package com.socsentinel.portal.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "login_activity")
public class LoginActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    private String role;

    @Column(nullable = false)
    private LocalDateTime loginAt;

    private LocalDateTime logoutAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginStatus status;

    private String ipAddress;
    private String sessionId;
    private Long sessionDurationSeconds;

    protected LoginActivity() {
    }

    public LoginActivity(String username, String role, LocalDateTime loginAt,
                         LoginStatus status, String ipAddress, String sessionId) {
        this.username = username;
        this.role = role;
        this.loginAt = loginAt;
        this.status = status;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public LocalDateTime getLoginAt() {
        return loginAt;
    }

    public LocalDateTime getLogoutAt() {
        return logoutAt;
    }

    public LoginStatus getStatus() {
        return status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Long getSessionDurationSeconds() {
        return sessionDurationSeconds;
    }

    public void closeSession(LocalDateTime logoutAt) {
        this.logoutAt = logoutAt;
        this.sessionDurationSeconds = Math.max(0, java.time.Duration.between(loginAt, logoutAt).getSeconds());
    }
}
