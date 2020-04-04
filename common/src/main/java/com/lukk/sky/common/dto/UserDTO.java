package com.lukk.sky.common.dto;

import com.lukk.sky.common.entity.Role;
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
