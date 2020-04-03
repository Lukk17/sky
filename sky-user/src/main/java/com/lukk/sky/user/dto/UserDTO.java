package com.lukk.sky.user.dto;

import com.lukk.sky.user.entity.Role;
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
