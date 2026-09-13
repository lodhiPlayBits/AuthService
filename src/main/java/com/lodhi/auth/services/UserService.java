package com.lodhi.auth.services;

import java.util.Set;
import java.util.UUID;

import com.lodhi.auth.dtos.UpdateUserRequestDTO;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;

public interface UserService {

    CreateUserResponseDTO createUser(CreateUserRequestDTO createUserRequestDTO);

    CreateUserResponseDTO getUserByEmail(String email);

    CreateUserResponseDTO getUserById(Long id);

    CreateUserResponseDTO updateUser(UpdateUserRequestDTO updateUserRequestDTO, Long id);

    void deleteUser(Long id);

    Iterable<CreateUserResponseDTO> getAllUsers();
    
    CreateUserResponseDTO assignRolesToUser(Long userId, Set<UUID> roleIds);
}
