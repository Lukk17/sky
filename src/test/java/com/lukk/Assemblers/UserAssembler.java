package com.lukk.Assemblers;

import com.google.gson.Gson;
import com.lukk.entity.User;

import java.util.ArrayList;

public class UserAssembler {

    public static final String TEST_USER_EMAIL = "testUser@user";

    public static User createTestUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setPassword("test");
        user.setReceivedMessage(new ArrayList<>());
        user.setSentMessage(new ArrayList<>());

        return user;
    }

    public static String createJsonUser(){
        User user = new User();
        user.setEmail(TEST_USER_EMAIL);
        user.setPassword("test");

        return new Gson().toJson(user);
    }

}
