package com.basic.interfaceService;

import com.basic.entity.User;

public interface UserServiceInterface {

    /**
     *  Search for user by his unique email address.
     *
     * @param email     User's email.
     * @return          Return only one user with given unique email.
     */
    User findByUserEmail(String email);

}
