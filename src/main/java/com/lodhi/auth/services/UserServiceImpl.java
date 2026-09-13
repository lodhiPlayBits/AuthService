package com.lodhi.auth.services;

import com.lodhi.auth.dtos.UpdateUserRequestDTO;
import com.lodhi.auth.dtos.UserRequestDTO;
import com.lodhi.auth.dtos.UserResponseDTO;
import com.lodhi.auth.enums.Provider;
import com.lodhi.auth.enums.RoleType;
import com.lodhi.auth.exceptions.registration.UserAlreadyExistsException;
import com.lodhi.auth.exceptions.resource.ResourceNotFoundException;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        // Check if user already exists
        if(userRepository.existsByEmail(userRequestDTO.getEmail())){
            throw new UserAlreadyExistsException("User with email " + userRequestDTO.getEmail() + " already exists");
        }
        if(userRepository.existsByUsername(userRequestDTO.getUserName())){
            throw new UserAlreadyExistsException("User with email " + userRequestDTO.getUserName() + " already exists");
        }
        if(userRepository.existsByUsername(userRequestDTO.getPhoneNumber())){
            throw new UserAlreadyExistsException("User with email " + userRequestDTO.getPhoneNumber() + " already exists");
        }
        
        // Map DTO to entity
        User user = modelMapper.map(userRequestDTO, User.class);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(userRequestDTO.getPassword()));

        // Assign default USER role
        Role userRole = roleService.getRole(RoleType.USER);
        user.setRoles(Set.of(userRole));

        // Set provider (default to LOCAL if not specified)
        user.setProvider(userRequestDTO.getProvider() != null ? userRequestDTO.getProvider() : Provider.LOCAL);

        // Save user
        User savedUser = userRepository.save(user);

        return modelMapper.map(savedUser, UserResponseDTO.class);
    }

    @Override
    public UserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return modelMapper.map(user, UserResponseDTO.class);
    }

    @Override
    public UserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return modelMapper.map(user, UserResponseDTO.class);
    }

    @Override
    public UserResponseDTO updateUser(UpdateUserRequestDTO updateUserRequestDTO, Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        
        modelMapper.map(updateUserRequestDTO, user);
        User updatedUser = userRepository.save(user);

        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public Iterable<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserResponseDTO.class))
                .toList();
    }
}
