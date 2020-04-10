package com.lukk.sky.authservice.Assemblers;

import com.google.gson.Gson;
import com.lukk.sky.authservice.dto.UserDTO;
import com.lukk.sky.authservice.entity.User;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class UserAssembler {

    public static final String TEST_USER_EMAIL = "testUser@user";
    public static final String SECOND_TEST_USER_EMAIL = "testUser2@user";

    public static final String TEST_USER_PASSWORD = "test";


    public static User createSimpleTestUser(String email) {
        return User.builder()
                .email(email)
                .password(TEST_USER_PASSWORD)
                .build();
    }

    public static User createCompleteTestUser(String email, Long id) {
        return User.builder()
                .id(id)
                .email(email)
                .password(TEST_USER_PASSWORD)
                .roles(new HashSet<>(Collections.singletonList(RoleAssembler.getUserRole())))
                .build();
    }

    public static UserDTO createTestUserDTO_withPassword(String email) {
        return UserDTO.builder()
                .email(email)
                .password(TEST_USER_PASSWORD)
                .build();
    }

    public static UserDTO createTestUserDTO_withoutPassword(String email) {
        return UserDTO.builder()
                .email(email)
                .build();
    }

    public static UserDTO createCompleteTestUserDTO(String email, Long id) {
        return UserDTO.builder()
                .id(id)
                .email(email)
                .password(TEST_USER_PASSWORD)
                .roles(new HashSet<>(Collections.singletonList(RoleAssembler.getUserRole())))
                .build();
    }

    public static String createJsonUser() {
        User user = new User();
        user.setEmail(TEST_USER_EMAIL);
        user.setPassword(TEST_USER_PASSWORD);

        return new Gson().toJson(user);
    }

    /**
     * For Auth User have to have field username not email.
     *
     * @return Json as String
     */
    public static String createJsonUserForAuth() {
        return new Gson().toJson(new AuthUser(TEST_USER_EMAIL, SECOND_TEST_USER_EMAIL));
    }

    public static List<User> createUserList() {
        return Arrays.asList(
                UserAssembler.createCompleteTestUser(TEST_USER_EMAIL, 1L),
                UserAssembler.createCompleteTestUser(SECOND_TEST_USER_EMAIL, 2L));
    }

    public static List<UserDTO> createUserDTOList() {
        return Arrays.asList(
                UserAssembler.createCompleteTestUserDTO(TEST_USER_EMAIL, 1L),
                UserAssembler.createCompleteTestUserDTO(SECOND_TEST_USER_EMAIL, 2L));
    }

    @RequiredArgsConstructor
    private static class AuthUser {
        private final String username;
        private final String password;

    }
}
