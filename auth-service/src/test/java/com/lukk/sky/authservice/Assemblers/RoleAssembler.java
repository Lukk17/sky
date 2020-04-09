package com.lukk.sky.authservice.Assemblers;

import com.lukk.sky.authservice.entity.Role;

import java.util.ArrayList;

public class RoleAssembler {

    public static String USER_ROLE_NAME = "USER";
    public static String ADMIN_ROLE_NAME = "ADMIN";
    public static Long USER_ROLE_ID = 1L;
    public static Long ADMIN_ROLE_ID = 0L;

    public static Role getUserRole() {
        Role result = new Role();
        result.setName(USER_ROLE_NAME);
        result.setId(USER_ROLE_ID);
        result.setUserList(new ArrayList<>());
        return result;
    }

    public static Role getAdminRole() {
        Role result = new Role();
        result.setName(ADMIN_ROLE_NAME);
        result.setId(ADMIN_ROLE_ID);
        result.setUserList(new ArrayList<>());
        return result;
    }
}
