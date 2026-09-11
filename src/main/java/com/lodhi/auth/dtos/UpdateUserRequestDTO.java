package com.lodhi.auth.dtos;

import com.lodhi.auth.enums.Gender;
import com.lodhi.auth.enums.Provider;
import lombok.Data;

@Data
public class UpdateUserRequestDTO {
    private String name;
    private String email;
    private String password;
    private String phoneNumber;
    private Gender gender;
    private String image;
}
