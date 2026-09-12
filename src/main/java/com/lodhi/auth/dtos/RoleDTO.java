package com.lodhi.auth.dtos;


import com.lodhi.auth.enums.RoleType;
import lombok.*;

import java.util.UUID;


@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class RoleDTO {

    private String roleName;
}
