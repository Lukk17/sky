package com.lukk.sky.authservice.dto;

import com.lukk.sky.authservice.entity.Role;
import com.lukk.sky.authservice.entity.User;
import com.lukk.sky.authservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;

@Component
@RequiredArgsConstructor
public class EntityDTOConverter {

    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserDTO convertUserEntity_toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .roles(user.getRoles())
                .build();

    }

    public User convertUserDTO_toEntity(UserDTO userDTO) {
        Role userRole = roleRepository.findByName("ROLE_USER");

        return User.builder()
                .email(userDTO.getEmail())
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

    }
}
