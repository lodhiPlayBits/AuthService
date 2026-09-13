package com.lodhi.auth.services;

import com.lodhi.auth.dtos.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    UserResponseDTO registeruser(UserRequestDTO userRequestDTO);

    LoginResponseDTO login(LoginRequestDTO loginRequestDTO, HttpServletResponse response, HttpServletRequest request);
}
