package com.lodhi.auth.security;

import java.util.Optional;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.lodhi.auth.enums.IdentifiersTypes;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.UserRepository;
import com.lodhi.auth.utils.IdentifierUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final IdentifierUtils identifierUtils;

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        IdentifiersTypes type = identifierUtils.resolveType(identifier);

        // Use fetch join queries to eagerly load roles and permissions
        // This prevents N+1 queries during authentication
        Optional<User> userOptional = switch (type) {
            case EMAIL -> userRepository.findByEmailWithRolesAndPermissions(identifier);
            case PHONE -> userRepository.findByPhoneNumber(identifier);  // Phone login doesn't need roles immediately
            case USERNAME -> userRepository.findByUsernameWithRolesAndPermissions(identifier);
        };

        User user = userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return user;
    }
}