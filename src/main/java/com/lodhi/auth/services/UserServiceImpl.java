package com.lodhi.auth.services;

import com.lodhi.auth.dtos.UpdateUserRequestDTO;
import com.lodhi.auth.dtos.UserRequestDTO;
import com.lodhi.auth.dtos.UserResponseDTO;
import com.lodhi.auth.enums.Provider;
import com.lodhi.auth.exceptions.BlankFieldException;
import com.lodhi.auth.exceptions.ResourceNotFoundException;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;


    @Override
    public UserResponseDTO createUser(UserRequestDTO userRequestDTO) {
        if(userRequestDTO.getEmail()==null || userRequestDTO.getEmail().isBlank()){
            throw new BlankFieldException("Email is required");
        }
        if(userRequestDTO.getName()==null || userRequestDTO.getName().isBlank()){
            throw new BlankFieldException("Name is required");
        }

        if(userRequestDTO.getPhoneNumber()==null || userRequestDTO.getPhoneNumber().isBlank()){
            throw new BlankFieldException("Phone Number is required");
        }
        if(userRequestDTO.getPassword()==null || userRequestDTO.getPassword().isBlank()){
            throw new BlankFieldException("Password is required");
        }

        if(userRepository.existsByEmail(userRequestDTO.getEmail())){
            throw new IllegalArgumentException("Email Already existed");
        }
        
        User user=modelMapper.map(userRequestDTO,User.class);
        user.setEnable(true);

        user.setProvider(userRequestDTO.getProvider()!=null ? userRequestDTO.getProvider(): Provider.LOCAL);

        User savedUser=userRepository.save(user);


        return modelMapper.map(savedUser, UserResponseDTO.class);

    }

    @Override
    public UserResponseDTO getUserByEmail(String email) {

        Optional<User> user= userRepository.findByEmail(email);
        if(user.isEmpty()){
            throw new ResourceNotFoundException("Email not found");
        }
        System.out.println(user);
        return modelMapper.map(user.get(),UserResponseDTO.class);
    }

    @Override
    public UserResponseDTO getUserById(Long id) {
        Optional<User> user= userRepository.findById(id);
        if(user.isEmpty()){
            throw new ResourceNotFoundException("Email not found");
        }
        return modelMapper.map(user.get(),UserResponseDTO.class);
    }

    @Override
    public UserResponseDTO updateUser(UpdateUserRequestDTO updateUserRequestDTO, Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User does not exist"));
        System.out.println("Before: " + user.getName());
        modelMapper.map(updateUserRequestDTO, user);
        System.out.println("After: " + user.getName());
        User updatedUser = userRepository.save(user);

        return modelMapper.map(updatedUser, UserResponseDTO.class);
    }

    @Override
    public void deleteUser(Long id) {
        User u=userRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("User does not exit"));
        userRepository.deleteById(id);
    }

    @Override
    public Iterable<UserResponseDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user ->modelMapper.map(user,UserResponseDTO.class))
                .toList();
    }
}
