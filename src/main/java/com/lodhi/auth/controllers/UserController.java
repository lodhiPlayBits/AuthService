package com.lodhi.auth.controllers;


import com.lodhi.auth.dtos.EmailUserRequestDTO;
import com.lodhi.auth.dtos.UpdateUserRequestDTO;
import com.lodhi.auth.dtos.UserRequestDTO;
import com.lodhi.auth.dtos.UserResponseDTO;
import com.lodhi.auth.services.UserService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor

public class UserController {

    private  final UserService userService;

    @PostMapping("/create-user")
    public ResponseEntity<UserResponseDTO>createUser( @RequestBody UserRequestDTO userRequestDTO){
        return ResponseEntity.ok(userService.createUser(userRequestDTO));

    }

    @GetMapping
    public ResponseEntity<Iterable<UserResponseDTO>>getAllUser(){
        return ResponseEntity.status(HttpStatus.OK).body(userService.getAllUsers());
    }

    @GetMapping("/getByEmail")
    public ResponseEntity<UserResponseDTO>getUserByEmail(@RequestBody EmailUserRequestDTO emailUserRequestDTO){
        return ResponseEntity.status(HttpStatus.FOUND).body(userService.getUserByEmail(emailUserRequestDTO.getEmail()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO>updateUserByid(@RequestBody UpdateUserRequestDTO updateUserRequestDTO, @PathVariable Long id){
        return ResponseEntity.status(HttpStatus.OK).body(userService.updateUser( updateUserRequestDTO,id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO>getUserById( @PathVariable Long id){
        return ResponseEntity.status(HttpStatus.FOUND).body(userService.getUserById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserByid(@PathVariable Long id){
        userService.deleteUser(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
