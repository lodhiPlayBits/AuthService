package com.lodhi.auth.dtos;


import com.lodhi.auth.enums.Gender;
import com.lodhi.auth.enums.Provider;

import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder

public class UserResponseDTO {



    private Long Id;

    private String email;

    private String name;


    private Gender gender;

    private String phoneNumber;

    private String image;


    private Instant createdAt= Instant.now() ;

    private Instant updatedA=Instant.now();


    private Provider provider=Provider.LOCAL;

    private Set<RoleDTO> roles=new HashSet<>();


}
