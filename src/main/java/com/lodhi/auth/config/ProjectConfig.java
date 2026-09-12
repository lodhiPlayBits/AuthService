package com.lodhi.auth.config;

import com.lodhi.auth.dtos.RoleDTO;
import com.lodhi.auth.model.Role;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectConfig {

    @Bean
    public ModelMapper modelMapper(){
        ModelMapper modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        
        // Custom converter for Role to RoleDTO
        Converter<Role, RoleDTO> roleToRoleDTOConverter = context -> {
            Role source = context.getSource();
            RoleDTO destination = new RoleDTO();
            if (source != null && source.getRoleType() != null) {
                destination.setRoleName(source.getRoleType().name());
            }
            return destination;
        };
        
        modelMapper.createTypeMap(Role.class, RoleDTO.class)
                .setConverter(roleToRoleDTOConverter);
        
        return modelMapper;
    }
}
