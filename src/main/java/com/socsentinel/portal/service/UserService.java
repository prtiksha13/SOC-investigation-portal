package com.socsentinel.portal.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socsentinel.portal.model.User;
import com.socsentinel.portal.repository.UserRepository;

@Service
public class UserService implements UserDetailsService {
    private static final java.util.Set<String> ALLOWED_USERNAMES =
            java.util.Set.of("analyst1", "analyst2", "admin");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByUsername(username);
        return org.springframework.security.core.userdetails.User.withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }

    public User findByUsername(String username) {
        if (!ALLOWED_USERNAMES.contains(username)) {
            throw new UsernameNotFoundException("User not found");
        }
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public boolean userExists(String username) {
        return ALLOWED_USERNAMES.contains(username) && userRepository.findByUsername(username).isPresent();
    }

    @Transactional
    public void resetPassword(String username, String newPassword) {
        User user = findByUsername(username);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
