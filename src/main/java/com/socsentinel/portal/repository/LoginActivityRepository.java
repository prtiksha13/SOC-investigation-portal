package com.socsentinel.portal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.socsentinel.portal.model.LoginActivity;

public interface LoginActivityRepository extends JpaRepository<LoginActivity, Long> {
    List<LoginActivity> findAllByOrderByLoginAtDesc();
    Optional<LoginActivity> findFirstBySessionIdAndStatusAndLogoutAtIsNullOrderByLoginAtDesc(
            String sessionId, com.socsentinel.portal.model.LoginStatus status);
}
