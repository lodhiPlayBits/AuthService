package com.lodhi.auth.services;

import com.lodhi.auth.dtos.LoginRequestDTO;
import com.lodhi.auth.dtos.LoginResponseDTO;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    CreateUserResponseDTO registeruser(CreateUserRequestDTO createUserRequestDTO);

    LoginResponseDTO login(LoginRequestDTO loginRequestDTO, HttpServletResponse response, HttpServletRequest request);

    /**
     * Logout current session by revoking the refresh token family.
     * 
     * @param refreshToken The refresh token to revoke
     * @param request HTTP request for audit logging
     * @param response HTTP response to clear refresh token cookie
     */
    void logout(String refreshToken, HttpServletRequest request, HttpServletResponse response);

    /**
     * Logout from all devices by revoking all refresh tokens for the user.
     * 
     * @param userId User ID to logout from all devices
     * @param request HTTP request for audit logging
     * @param response HTTP response to clear refresh token cookie
     */
    void logoutAllDevices(Long userId, HttpServletRequest request, HttpServletResponse response);
}
