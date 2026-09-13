package com.lodhi.auth.services;

import java.util.Locale;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.dtos.UpdateUserRequestDTO;
import com.lodhi.auth.enums.Provider;
import com.lodhi.auth.enums.RoleType;
import com.lodhi.auth.exceptions.registration.UserAlreadyExistsException;
import com.lodhi.auth.exceptions.resource.ResourceNotFoundException;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public CreateUserResponseDTO createUser(CreateUserRequestDTO createUserRequestDTO) {
        // Check if user already exists
        String email = createUserRequestDTO.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = createUserRequestDTO.getUsername().trim().toLowerCase(Locale.ROOT);
        String phoneNumber = createUserRequestDTO.getPhoneNumber().trim().toLowerCase(Locale.ROOT);


        if(userRepository.existsByEmail(email)){
            throw new UserAlreadyExistsException("User with email " + createUserRequestDTO.getEmail() + " already exists");
        }
        if(userRepository.existsByUsername(username)){
            throw new UserAlreadyExistsException("User with username " + createUserRequestDTO.getUsername() + " already exists");
        }
        if(userRepository.existsByPhoneNumber(phoneNumber)){
            throw new UserAlreadyExistsException("User with phone number " + createUserRequestDTO.getPhoneNumber() + " already exists");
        }
        
        // Map DTO to entity
        User user = modelMapper.map(createUserRequestDTO, User.class);
        user.setEnabled(true);
        user.setPassword(passwordEncoder.encode(createUserRequestDTO.getPassword()));

        // Assign default USER role
        Role userRole = roleService.getRole(RoleType.USER);
        user.setRoles(Set.of(userRole));

        // Set provider (default to LOCAL if not specified)
        user.setProvider(createUserRequestDTO.getProvider() != null ? createUserRequestDTO.getProvider() : Provider.LOCAL);

        // Save user
        User savedUser = userRepository.save(user);

        return modelMapper.map(savedUser, CreateUserResponseDTO.class);
    }

    @Override
    public CreateUserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return modelMapper.map(user, CreateUserResponseDTO.class);
    }

    @Override
    public CreateUserResponseDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return modelMapper.map(user, CreateUserResponseDTO.class);
    }

    @Override
    public CreateUserResponseDTO updateUser(UpdateUserRequestDTO updateUserRequestDTO, Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        
        modelMapper.map(updateUserRequestDTO, user);
        User updatedUser = userRepository.save(user);

        return modelMapper.map(updatedUser, CreateUserResponseDTO.class);
    }

    @Override
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public Iterable<CreateUserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> modelMapper.map(user, CreateUserResponseDTO.class))
                .toList();
    }

    @Override
    public CreateUserResponseDTO assignRolesToUser(Long userId, Set<java.util.UUID> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        Set<Role> roles = roleIds.stream()
                .map(roleService::getRoleById)
                .collect(java.util.stream.Collectors.toSet());
        
        user.setRoles(roles);
        User updatedUser = userRepository.save(user);
        
        return modelMapper.map(updatedUser, CreateUserResponseDTO.class);
    }
}
