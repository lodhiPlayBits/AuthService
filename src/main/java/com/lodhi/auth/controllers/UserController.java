package com.lodhi.auth.controllers;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lodhi.auth.dtos.EmailUserRequestDTO;
import com.lodhi.auth.dtos.UpdateUserRequestDTO;
import com.lodhi.auth.dtos.request.CreateUserRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.services.UserService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {

    private final UserService userService;

    // Admin only - create user with specific role
    @PostMapping("/create-user")
    @PreAuthorize("hasAuthority('admin:create')")
    public ResponseEntity<CreateUserResponseDTO> createUser(@RequestBody CreateUserRequestDTO createUserRequestDTO){
        return ResponseEntity.ok(userService.createUser(createUserRequestDTO));
    }

    @GetMapping("/getByEmail")
    @PreAuthorize("hasAuthority('admin:read') or hasAuthority('user:read')")
    public ResponseEntity<CreateUserResponseDTO> getUserByEmail(@RequestBody EmailUserRequestDTO emailUserRequestDTO){
        return ResponseEntity.status(HttpStatus.FOUND).body(userService.getUserByEmail(emailUserRequestDTO.getEmail()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:update') or (hasAuthority('user:update') and #id == authentication.principal.id)")
    public ResponseEntity<CreateUserResponseDTO> updateUserByid(@RequestBody UpdateUserRequestDTO updateUserRequestDTO, @PathVariable Long id){
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser(updateUserRequestDTO, id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:read') or (hasAuthority('user:read') and #id == authentication.principal.id)")
    public ResponseEntity<CreateUserResponseDTO> getUserById(@PathVariable Long id){
        return ResponseEntity.status(HttpStatus.FOUND).body(userService.getUserById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:delete') or (hasAuthority('user:delete') and #id == authentication.principal.id)")
    public ResponseEntity<Void> deleteUserByid(@PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
