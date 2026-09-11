package com.lodhi.auth.services;

import com.lodhi.auth.dtos.LoginRequestDTO;
import com.lodhi.auth.dtos.TokenResponse;
import com.lodhi.auth.dtos.UserRequestDTO;
import com.lodhi.auth.dtos.UserResponseDTO;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    UserResponseDTO registeruser(UserRequestDTO userRequestDTO);

    TokenResponse login(LoginRequestDTO loginRequestDTO, HttpServletResponse response);
}
