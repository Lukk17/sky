package com.lukk.sky.common.service;

import com.lukk.sky.common.dto.UserDTO;
import com.lukk.sky.common.entity.User;

import java.util.List;


public interface UserService {

    /**
     * Search for user by his unique email address.
     *
     * @param email User's email.
     * @return Return only one user with given unique email.
     */
    User findByUserEmail(String email);

    /**
     * Give user with ID same as given.
     *
     * @param id ID of user.
     * @return Return one User.
     */
    User findById(Long id);


    /**
     * @return all registered users as list
     */
    List<User> findAll();

    List<UserDTO> findAllAndConvertToDTO();

    /**
     * Save new, given User.
     * Encode password with BCryptPasswordEncoder.
     * Set Role to ROLE_USER.
     * Set default photo.
     *
     * @param user User which will be saved.
     * @return User that was saved to DB
     */
    User saveUser(UserDTO user);


    /**
     * Delete User with given ID.
     *
     * @param id ID of user to delete.
     */
    void deleteUser(Long id);

    /**
     * Check if given newPassword is same as saved in database.
     * Used in login.
     *
     * @param newPassword Password from login.
     * @param password    Password from database.
     * @return Return true if passwords are same.
     */
    boolean checkPassword(String newPassword, String password);

    UserDTO findUserDetails(String email);
}
