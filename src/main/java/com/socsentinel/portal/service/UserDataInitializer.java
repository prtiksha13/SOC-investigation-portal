package com.socsentinel.portal.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.socsentinel.portal.model.User;
import com.socsentinel.portal.repository.UserRepository;

@Configuration
public class UserDataInitializer {
    @Bean
    CommandLineRunner initializeUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            userRepository.findAll().stream()
                    .filter(user -> !java.util.Set.of("analyst1", "analyst2", "admin").contains(user.getUsername()))
                    .forEach(userRepository::delete);
            ensureUser(userRepository, passwordEncoder, "analyst1", "ANALYST_L1", "password");
            ensureUser(userRepository, passwordEncoder, "analyst2", "ANALYST_L2", "password");
            ensureUser(userRepository, passwordEncoder, "admin", "ADMIN", "admin123");
        };
    }

    private void ensureUser(UserRepository userRepository, PasswordEncoder passwordEncoder,
                            String username, String role, String initialPassword) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            userRepository.save(new User(username, passwordEncoder.encode(initialPassword), role));
            return;
        }
        if (!role.equals(user.getRole())) {
            user.setRole(role);
            userRepository.save(user);
        }
        if ("admin".equals(username) && passwordEncoder.matches("password", user.getPassword())) {
            user.setPassword(passwordEncoder.encode(initialPassword));
            userRepository.save(user);
        }
    }
}
