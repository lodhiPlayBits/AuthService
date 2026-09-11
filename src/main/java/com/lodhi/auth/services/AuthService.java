package com.lodhi.auth.services;

import com.lodhi.auth.dtos.UserRequestDTO;
import com.lodhi.auth.dtos.UserResponseDTO;

public interface AuthService {

    UserResponseDTO registeruser(UserRequestDTO userRequestDTO);
}
