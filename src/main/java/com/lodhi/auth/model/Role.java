package com.lodhi.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.util.UUID;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
public class Role {

    @Id
    private UUID id=UUID.randomUUID();
    @Column(unique = true, nullable = false)
    private String roleName;
}
