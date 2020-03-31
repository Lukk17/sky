package com.lukk.Assemblers;

import com.google.gson.Gson;
import com.lukk.dto.UserDTO;
import com.lukk.entity.Role;
import com.lukk.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class UserAssembler {

    public static final String TEST_USER_EMAIL = "testUser@user";

    public static User createTestUser(String email) {
        User user = User.builder()
                .email(email)
                .password("test")
//                .receivedMessage(new ArrayList<>())
//                .sentMessage(new ArrayList<>())
                .build();
        return user;
    }

    public static UserDTO createTestUserDTO(String email) {
        UserDTO user = UserDTO.builder()
                .email(email)
                .password("test")
                .build();
        return user;
    }

    public static String createJsonUser() {
        User user = new User();
        user.setEmail(TEST_USER_EMAIL);
        user.setPassword("test");

        return new Gson().toJson(user);
    }

    public static User convertUserDTO_toEntity(UserDTO userDTO) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        Role userRole = new Role(1L, "USER", new ArrayList<User>());

        return User.builder()
                .email(userDTO.getEmail())
                .password(passwordEncoder.encode(userDTO.getPassword()))
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

    }

}
