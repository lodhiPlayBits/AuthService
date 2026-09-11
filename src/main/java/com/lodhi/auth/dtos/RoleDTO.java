package com.lodhi.auth.dtos;


import jakarta.persistence.Entity;

import lombok.*;

import java.util.UUID;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder

public class RoleDTO {

    private UUID id;
    private String role_name;
}
