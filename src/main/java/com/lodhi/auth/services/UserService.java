package com.lodhi.auth.services;

import com.lodhi.auth.dtos.UpdateUserRequestDTO;
import com.lodhi.auth.dtos.UserRequestDTO;
import com.lodhi.auth.dtos.UserResponseDTO;

public interface UserService {

    UserResponseDTO createUser(UserRequestDTO userRequestDTO);

    UserResponseDTO getUserByEmail(String email);

    UserResponseDTO getUserById(Long id);

    UserResponseDTO updateUser(UpdateUserRequestDTO updateUserRequestDTO, Long id);

    void deleteUser(Long id);

    Iterable<UserResponseDTO>getAllUsers();



}
