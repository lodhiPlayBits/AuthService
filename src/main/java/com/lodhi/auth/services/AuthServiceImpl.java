package com.lodhi.auth.services;


import com.lodhi.auth.dtos.UserRequestDTO;
import com.lodhi.auth.dtos.UserResponseDTO;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private  final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDTO registeruser(UserRequestDTO userRequestDTO){
        userRequestDTO.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));
        return userService.createUser(userRequestDTO);
    }


}
