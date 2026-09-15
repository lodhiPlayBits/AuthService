package com.lodhi.auth.initialization;

import java.util.Set;

import com.lodhi.auth.constants.SystemRoles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.lodhi.auth.enums.Gender;
import com.lodhi.auth.enums.Provider;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RoleRepository;
import com.lodhi.auth.respositories.UserRepository;

import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:Admin@admin.com}")
    private String adminEmail;

    @Value("${admin.username:Admin}")
    private String adminUsername;

    @Value("${admin.password:admin}")
    private String adminPassword;

    @Value("${admin.phoneNumber:8595007855}")
    private String adminPhoneNumber;

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        Role adminRole = roleRepository.findByName(SystemRoles.ADMIN).orElseThrow(()->new IllegalStateException("Admin Role not found"));

        User admin=new User();

        admin.setUsername(adminUsername);
        admin.setEmail(adminEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRoles(Set.of(adminRole));
        admin.setName(adminUsername);
        admin.setGender(Gender.MALE);
        admin.setEnabled(true);
        admin.setPhoneNumber(adminPhoneNumber);
        admin.setProvider(Provider.LOCAL);

        userRepository.save(admin);


    }
}
