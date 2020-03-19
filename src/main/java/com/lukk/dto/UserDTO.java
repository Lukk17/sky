package com.lukk.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.lukk.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserDTO {

    private Long id;
    private String email;

    //  password is needed to adding new user,
    //  but it should not be displayed when getting userList
    @JsonIgnore
    private String password;

    private Set<Role> roles;

}
