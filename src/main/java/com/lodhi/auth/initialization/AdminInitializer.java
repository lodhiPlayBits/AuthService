package com.lodhi.auth.initialization;

import com.lodhi.auth.enums.Gender;
import com.lodhi.auth.enums.Provider;
import com.lodhi.auth.enums.RoleType;
import com.lodhi.auth.model.Role;
import com.lodhi.auth.model.User;
import com.lodhi.auth.respositories.RoleRepository;
import com.lodhi.auth.respositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;


@RequiredArgsConstructor
@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.existsByEmail("Admin@admin.com")) {
            return;
        }

        Role adminRole = roleRepository.findByRoleType(RoleType.ADMIN).orElseThrow(()->new IllegalStateException("Admin Role not found"));

        User admin=new User();

        admin.setEmail("Admin@admin.com");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setRoles(Set.of(adminRole));
        admin.setName("Admin");
        admin.setGender(Gender.MALE);
        admin.setEnable(true);
        admin.setPhoneNumber("8595007855");
        admin.setProvider(Provider.LOCAL);

        userRepository.save(admin);


    }
}
