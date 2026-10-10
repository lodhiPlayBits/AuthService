package com.lodhi.auth.services;

import java.util.Locale;
import java.util.Set;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
import com.lodhi.auth.respositories.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final RoleServiceImpl roleService;  // impl for getRoleEntityByName()
    private final PasswordEncoder passwordEncoder;
    private final BCryptService bcryptService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountEventProducer accountEventProducer;
    private final UserServiceImpl self;
    
    public UserServiceImpl(
            UserRepository userRepository,
            ModelMapper modelMapper,
            RoleServiceImpl roleService,
            PasswordEncoder passwordEncoder,
            BCryptService bcryptService,
            RefreshTokenRepository refreshTokenRepository,
            AccountEventProducer accountEventProducer,
            @Lazy UserServiceImpl self) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.bcryptService = bcryptService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accountEventProducer = accountEventProducer;
        this.self = self;
    }

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

        // Assign default USER role — save user FIRST to get DB user_id (FK constraint),
        // then attach role, same pattern used in AdminInitializer and GoogleOAuth2Service.
        Role userRole = roleService.getRoleEntityByName(SystemRoles.USER);

        // Set provider (default to LOCAL if not specified)
        user.setProvider(createUserRequestDTO.getProvider() != null ? createUserRequestDTO.getProvider() : Provider.LOCAL);

        // Save user - catch DB constraint violations for concurrent registrations
        try {
            User savedUser = userRepository.saveAndFlush(user);
            savedUser.setRoles(new java.util.HashSet<>(Set.of(userRole)));
            savedUser = userRepository.save(savedUser);
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
    @Transactional
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
        
        if (updateUserRequestDTO.getUsername() != null && !updateUserRequestDTO.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(updateUserRequestDTO.getUsername())) {
                throw new ValidationException("Username is already taken");
            }
            user.setUsername(updateUserRequestDTO.getUsername());
        }
        if (updateUserRequestDTO.getEmail() != null && !updateUserRequestDTO.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(updateUserRequestDTO.getEmail())) {
                throw new ValidationException("Email is already registered");
            }
            user.setEmail(updateUserRequestDTO.getEmail());
        }
        if (updateUserRequestDTO.getPhoneNumber() != null && !updateUserRequestDTO.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(updateUserRequestDTO.getPhoneNumber())) {
                throw new ValidationException("Phone number is already in use");
            }
            user.setPhoneNumber(updateUserRequestDTO.getPhoneNumber());
        }
        User updatedUser = userRepository.save(user);

        return modelMapper.map(updatedUser, CreateUserResponseDTO.class);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequestDTO requestDTO) {
        // Load user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // OAuth users cannot change password (they don't have one)
        if (user.getProvider() != Provider.LOCAL) {
            throw new ValidationException(
                "Password change is not available for " + user.getProvider()
                + " accounts. Please manage your password through your OAuth provider."
            );
        }

        // Validate passwords match
        if (!requestDTO.getNewPassword().equals(requestDTO.getConfirmPassword())) {
            throw new ValidationException("New password and confirmation do not match");
        }
        
        // Verify current password via bounded queue
        if (!bcryptService.matches(requestDTO.getCurrentPassword(), user.getPassword())) {
            throw new ValidationException("Current password is incorrect");
        }
        
        // Prevent reusing the same password
        if (bcryptService.matches(requestDTO.getNewPassword(), user.getPassword())) {
            throw new ValidationException("New password must be different from current password");
        }
        
        // Save using separate transactional method
        self.saveNewPassword(userId, passwordEncoder.encode(requestDTO.getNewPassword()));
    }

    @Transactional
    protected void saveNewPassword(Long userId, String encodedPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setPassword(encodedPassword);
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional
    public CreateUserResponseDTO assignRolesToUser(Long userId, Set<java.util.UUID> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        Set<Role> roles = roleIds.stream()
                .map(roleService::getRoleEntityById)
                .collect(java.util.stream.Collectors.toSet());
        
        user.setRoles(new java.util.HashSet<>(roles));
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

    @Override
    @Transactional
    public void updateUserStatus(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        user.setEnabled(enabled);
        userRepository.save(user);
        
        // If account is disabled, revoke all existing sessions immediately
        if (!enabled) {
            refreshTokenRepository.revokeAllForUser(userId);
        }

        // Defer until commit — a rolled-back status change must not emit
        // events that force clients to log out for nothing
        String email = user.getEmail();
        afterCommit(() -> {
            if (!enabled) {
                accountEventProducer.publishAccountDisabled(
                    userId,
                    email,
                    "Your account has been disabled by an administrator. Please contact support."
                );
            } else {
                accountEventProducer.publishAccountEnabled(userId, email);
            }
        });
    }

    private void afterCommit(Runnable action) {
        // registerSynchronization() throws when no transaction is active;
        // direct callers (e.g. unit tests) fall back to immediate execution
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
