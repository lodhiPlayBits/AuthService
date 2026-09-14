package com.lodhi.auth.services;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.lodhi.auth.dtos.ChangePasswordRequestDTO;
import com.lodhi.auth.dtos.UpdateUserRequestDTO;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;

public interface UserService {

    CreateUserResponseDTO createUser(CreateUserRequestDTO createUserRequestDTO);

    CreateUserResponseDTO getUserByEmail(String email);

    CreateUserResponseDTO getUserById(Long id);

    CreateUserResponseDTO updateUser(UpdateUserRequestDTO updateUserRequestDTO, Long id);

    void deleteUser(Long id);

    /**
     * Get all users with pagination support.
     * Maximum page size is enforced to prevent unbounded queries.
     * 
     * @param pageable pagination parameters (page number, size, sort)
     * @return paginated list of users
     */
    Page<CreateUserResponseDTO> getAllUsers(Pageable pageable);
    
    CreateUserResponseDTO assignRolesToUser(Long userId, Set<UUID> roleIds);
    
    /**
     * Change user password with proper verification.
     * Requires current password to prevent abuse if session is compromised.
     * New password is BCrypt encoded before storage.
     */
    void changePassword(Long userId, ChangePasswordRequestDTO requestDTO);
}
