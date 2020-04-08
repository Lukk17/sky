package com.lukk.sky.authservice.dto;

import com.lukk.sky.authservice.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserDTO {

    private Long id;
    private String email;
    private String password;
    private Set<Role> roles;
}
