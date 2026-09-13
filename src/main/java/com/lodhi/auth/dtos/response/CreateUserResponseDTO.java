package com.lodhi.auth.dtos.response;


import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.lodhi.auth.dtos.RoleDTO;
import com.lodhi.auth.enums.Gender;
import com.lodhi.auth.enums.Provider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder

public class CreateUserResponseDTO {

    private Long Id;

    private String email;

    private String name;

    private Gender gender;

    private String phoneNumber;

    private String image;

    private boolean enabled;

    @Builder.Default
    private Instant createdAt = Instant.now();

    private Instant updatedAt;

    @Builder.Default
    private Provider provider = Provider.LOCAL;

    @Builder.Default
    private Set<RoleDTO> roles = new HashSet<>();
}
