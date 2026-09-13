package com.lodhi.auth.security;

import com.lodhi.auth.dtos.LoginRequestDTO;
import com.lodhi.auth.enums.IdentifiersTypes;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.UserRepository;
import com.lodhi.auth.utils.IdentifierUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final IdentifierUtils identifierUtils;

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        IdentifiersTypes type = identifierUtils.resolveType(identifier);

        Optional<User> userOptional = switch (type) {
            case EMAIL -> userRepository.findByEmail(identifier);
            case PHONE -> userRepository.findByPhoneNumber(identifier);
            case USERNAME -> userRepository.findByUsername(identifier);
        };

        User user = userOptional.orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return user;
    }
}