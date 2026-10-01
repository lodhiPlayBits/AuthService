package com.lodhi.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.lodhi.auth.dtos.RoleDTO;
import com.lodhi.auth.model.Role;

import java.util.List;

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
            if (source != null && source.getName() != null) {
                destination.setRoleName(source.getName());
            }
            return destination;
        };
        
        modelMapper.createTypeMap(Role.class, RoleDTO.class)
                .setConverter(roleToRoleDTOConverter);
        
        return modelMapper;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Register JavaTimeModule for Java 8 date/time support
        mapper.registerModule(new JavaTimeModule());
        // Disable writing dates as timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow frontend origins - localhost for development and production domain
        configuration.setAllowedOrigins(List.of(
            "http://localhost:5173", 
            "http://localhost:3000",
            "https://authvolt.fun",
            "http://authvolt.fun"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); // Required for cookies and SSE
        configuration.setExposedHeaders(List.of("X-CSRF-TOKEN")); // Expose CSRF token
        configuration.setMaxAge(3600L); // Cache preflight response for 1 hour
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}

