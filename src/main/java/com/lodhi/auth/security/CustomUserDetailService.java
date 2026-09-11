package com.lodhi.auth.security;

import com.lodhi.auth.exceptions.ResourceNotFoundException;
import com.lodhi.auth.respositories.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;



@Service
@AllArgsConstructor
@Getter
@Setter
public class CustomUserDetailService implements UserDetailsService {
    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

         return userRepository.findByEmail(username).orElseThrow(()->new ResourceNotFoundException("User not found"));

    }

    @Service
    public static class jwtService {
    }
}
