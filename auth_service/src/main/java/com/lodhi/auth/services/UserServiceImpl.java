package com.lodhi.auth.services;

import java.util.Locale;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lodhi.auth.constants.SystemRoles;
import com.lodhi.auth.dtos.ChangePasswordRequestDTO;
import com.lodhi.auth.dtos.UpdateUserRequestDTO;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.enums.Provider;
import com.lodhi.auth.exceptions.registration.UserAlreadyExistsException;
import com.lodhi.auth.exceptions.resource.ResourceNotFoundException;
import com.lodhi.auth.exceptions.validation.ValidationException;
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
        // Normalize inputs
        String email = createUserRequestDTO.getEmail().trim().toLowerCase(Locale.ROOT);
        String username = createUserRequestDTO.getUsername().trim().toLowerCase(Locale.ROOT);
        String phoneNumber = createUserRequestDTO.getPhoneNumber().trim().toLowerCase(Locale.ROOT);

        // Pre-check for user-friendly error messages (not for uniqueness guarantee)
        // The database constraints are the authoritative uniqueness check
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
        Role userRole = roleService.getRoleByName(SystemRoles.USER);
        user.setRoles(Set.of(userRole));

        // Set provider (default to LOCAL if not specified)
        user.setProvider(createUserRequestDTO.getProvider() != null ? createUserRequestDTO.getProvider() : Provider.LOCAL);

        // Save user - catch DB constraint violations for concurrent registrations
        try {
            User savedUser = userRepository.save(user);
            return modelMapper.map(savedUser, CreateUserResponseDTO.class);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Race condition: constraint violation despite existsBy check
            // Parse constraint name to provide specific error message
            String message = ex.getMessage();
            
            if (message != null) {
                if (message.contains("email") || message.contains("user_table_email_key")) {
                    throw new UserAlreadyExistsException("User with email " + createUserRequestDTO.getEmail() + " already exists");
                } else if (message.contains("username") || message.contains("user_table_username_key")) {
                    throw new UserAlreadyExistsException("User with username " + createUserRequestDTO.getUsername() + " already exists");
                } else if (message.contains("phone_number") || message.contains("user_table_phone_number_key")) {
                    throw new UserAlreadyExistsException("User with phone number " + createUserRequestDTO.getPhoneNumber() + " already exists");
                }
            }
            
            // Generic uniqueness violation
            throw new UserAlreadyExistsException("User with provided credentials already exists");
        }
    }

    @Transactional(readOnly = true)
    @Override
    public CreateUserResponseDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return modelMapper.map(user, CreateUserResponseDTO.class);
    }

    @Transactional(readOnly = true)
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
        
        // Explicit field mapping - NO ModelMapper to prevent mass assignment
        // Only allow non-sensitive fields to be updated
        if (updateUserRequestDTO.getName() != null) {
            user.setName(updateUserRequestDTO.getName());
        }
        if (updateUserRequestDTO.getGender() != null) {
            user.setGender(updateUserRequestDTO.getGender());
        }
        if (updateUserRequestDTO.getImage() != null) {
            user.setImage(updateUserRequestDTO.getImage());
        }
        
        // Security-sensitive fields (password, email, phoneNumber) 
        // are NOT touched by this method
        
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
    public void changePassword(Long userId, ChangePasswordRequestDTO requestDTO) {
        // Validate passwords match
        if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
            throw new ValidationException("New password and confirmation do not match");
        }
        
        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        // Verify current password
        if (!passwordEncoder.matches(requestDTO.getCurrentPassword(), user.getPassword())) {
            throw new ValidationException("Current password is incorrect");
        }
        
        // Prevent reusing the same password
        if (passwordEncoder.matches(requestDTO.getNewPassword(), user.getPassword())) {
            throw new ValidationException("New password must be different from current password");
        }
        
        // Encode and set new password
        user.setPassword(passwordEncoder.encode(requestDTO.getNewPassword()));
        userRepository.save(user);
        
        // TODO: Add audit logging when AuditService is available
        // auditService.logServiceEvent(userId, username, AuditEventType.PASSWORD_CHANGED, true, "Password changed");
    }

    @Override
    public Page<CreateUserResponseDTO> getAllUsers(Pageable pageable) {
        // Enforce maximum page size to prevent unbounded queries
        int maxPageSize = 100;
        if (pageable.getPageSize() > maxPageSize) {
            pageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(), 
                maxPageSize, 
                pageable.getSort()
            );
        }
        
        Page<User> userPage = userRepository.findAll(pageable);
        return userPage.map(user -> modelMapper.map(user, CreateUserResponseDTO.class));
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
        
        // Audit log (this is a high-privilege operation)
        String username = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();
        String roleNames = roles.stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.joining(", "));
        // Note: Assuming auditService will be injected
        
        return modelMapper.map(updatedUser, CreateUserResponseDTO.class);
    }
}
