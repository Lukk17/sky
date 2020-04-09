package com.lukk.sky.authservice.service;

import com.lukk.sky.authservice.entity.Role;

public interface RoleService {
    /**
     * Give Role with same name as given.
     *
     * @param name Name of role.
     * @return Return one Role.
     */
    Role findByName(String name);

}
