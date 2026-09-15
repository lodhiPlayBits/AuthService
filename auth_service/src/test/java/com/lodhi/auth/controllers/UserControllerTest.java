package com.lodhi.auth.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.lodhi.auth.dtos.ChangePasswordRequestDTO;
import com.lodhi.auth.dtos.response.CreateUserResponseDTO;
import com.lodhi.auth.services.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void getUserById_ShouldReturnFound() {
        CreateUserResponseDTO res = new CreateUserResponseDTO();
        when(userService.getUserById(1L)).thenReturn(res);
        
        ResponseEntity<CreateUserResponseDTO> entity = userController.getUserById(1L);
        assertEquals(HttpStatus.FOUND, entity.getStatusCode());
        assertEquals(res, entity.getBody());
    }

    @Test
    void changePassword_ShouldReturnOk() {
        ChangePasswordRequestDTO req = new ChangePasswordRequestDTO();
        ResponseEntity<Void> entity = userController.changePassword(1L, req);
        
        verify(userService).changePassword(1L, req);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }
}
