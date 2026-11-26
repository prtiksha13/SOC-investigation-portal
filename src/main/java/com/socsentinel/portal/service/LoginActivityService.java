package com.socsentinel.portal.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socsentinel.portal.model.LoginActivity;
import com.socsentinel.portal.model.LoginStatus;
import com.socsentinel.portal.model.User;
import com.socsentinel.portal.repository.LoginActivityRepository;

@Service
public class LoginActivityService {
    private final LoginActivityRepository loginActivityRepository;
    private final UserService userService;

    public LoginActivityService(LoginActivityRepository loginActivityRepository, UserService userService) {
        this.loginActivityRepository = loginActivityRepository;
        this.userService = userService;
    }

    @Transactional
    public void recordSuccess(String username, String ipAddress, String sessionId) {
        User user = userService.findByUsername(username);
        loginActivityRepository.save(new LoginActivity(
                username, user.getRole(), LocalDateTime.now(), LoginStatus.SUCCESS, ipAddress, sessionId));
    }

    @Transactional
    public void recordFailure(String username, String ipAddress) {
        String role = null;
        if (userService.userExists(username)) {
            role = userService.findByUsername(username).getRole();
        }
        loginActivityRepository.save(new LoginActivity(
                username == null || username.isBlank() ? "unknown" : username,
                role, LocalDateTime.now(), LoginStatus.FAILED, ipAddress, null));
    }

    @Transactional
    public void recordLogout(String sessionId, LocalDateTime logoutAt) {
        if (sessionId == null) {
            return;
        }
        loginActivityRepository
                .findFirstBySessionIdAndStatusAndLogoutAtIsNullOrderByLoginAtDesc(sessionId, LoginStatus.SUCCESS)
                .ifPresent(activity -> {
                    activity.closeSession(logoutAt);
                    loginActivityRepository.save(activity);
                });
    }

    @Transactional(readOnly = true)
    public List<LoginActivity> findAll() {
        return loginActivityRepository.findAllByOrderByLoginAtDesc();
    }

    @Transactional(readOnly = true)
    public Map<String, LoginCount> countsByUsername() {
        Map<String, LoginCount> counts = new LinkedHashMap<>();
        counts.put("analyst1", new LoginCount(0, 0));
        counts.put("analyst2", new LoginCount(0, 0));
        counts.put("admin", new LoginCount(0, 0));
        Map<String, List<LoginActivity>> grouped = findAll().stream()
            .collect(Collectors.groupingBy(LoginActivity::getUsername));
        grouped.forEach((username, activities) -> counts.put(username, new LoginCount(
            activities.size(),
            activities.stream().filter(activity -> activity.getStatus() == LoginStatus.SUCCESS).count())));
        return counts;
    }

    public record LoginCount(long attempts, long successful) {
    }
}
