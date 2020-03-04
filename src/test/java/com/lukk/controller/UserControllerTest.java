package com.lukk.controller;

import com.google.gson.Gson;
import com.lukk.entity.User;
import com.lukk.service.SpringDataUserDetailsServiceImpl;
import com.lukk.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static com.lukk.Assemblers.UserAssembler.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @MockBean
    private SpringDataUserDetailsServiceImpl springDataUserDetailsService;


    @Test
    void getRegister() throws Exception {
        //Given
        User expected = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userService.saveUser(expected)).thenReturn(expected);

        //When
        mvc.perform(
                get("/registration")
                        .contentType(MediaType.APPLICATION_JSON))

                //Then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Give User"))
                );

    }

    @Test
    void putRegister() throws Exception {
        //Given
        User expected = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userService.saveUser(expected)).thenReturn(expected);

        //When
        mvc.perform(
                put("/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonUser()))
                //Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    void login() {

    }

    @Test
    @WithMockUser(username = "test@test", password = "test", roles = "USER")
    void userList() throws Exception {

        //Given
        User expected = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userService.findAll()).thenReturn(Collections.singletonList(expected));
        String expectedJson = "[{\"id\":null,\"roles\":null,\"receivedMessage\":[],\"sentMessage\":[],\"email\":\"testUser@user\",\"password\":\"test\"}]";


        //When
        mvc.perform(
                get("/userList")
                        .contentType(MediaType.APPLICATION_JSON))

                //Then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expectedJson))
                );

    }
}
