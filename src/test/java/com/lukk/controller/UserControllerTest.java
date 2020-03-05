package com.lukk.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(UserController.class)
class UserControllerTest {

    private final String DEFAULT_USER = "test@test";
    private final String DEFAULT_PASS = "test";


    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @MockBean
    private SpringDataUserDetailsServiceImpl springDataUserDetailsService;


    @Test
    void whenGetRegister_thenReturnString() throws Exception {
        //Given
        User expectedUser = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userService.saveUser(expectedUser)).thenReturn(expectedUser);

        //When
        mvc.perform(
                get("/registration")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())

                //Then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Give User"))
                );

    }

    @Test
    void whenPutRegister_thenAddNewUser() throws Exception {
        //Given
        User expectedUser = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userService.saveUser(expectedUser)).thenReturn(expectedUser);

        //When
        mvc.perform(
                put("/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonUser()))
                .andDo(print())

                //Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @WithMockUser(username = DEFAULT_USER, password = DEFAULT_PASS, roles = "USER")
    void whenLogin_thenReturnLoggedUser() throws Exception {
        //Given
        User expectedUser = createTestUser(TEST_USER_EMAIL);
        String expectedJson = "{\"email\":\"testUser@user\"}";
        Mockito.when(userService.findByUserEmail(DEFAULT_USER)).thenReturn(expectedUser);

        //When
        mvc.perform(
                get("/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())

                //Then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expectedJson))
                );
    }

    @Test
    void whenLoginWithoutCredentials_thenReturnLoggedUser() throws Exception {
        //Given
        User expectedUser = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userService.findByUserEmail(DEFAULT_USER)).thenReturn(expectedUser);

        //When
        mvc.perform(
                get("/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())

                //Then
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = DEFAULT_USER, password = DEFAULT_PASS, roles = "USER")
    void whenRequestUserList_andLogged_thenReturnListOfUsers() throws Exception {

        //Given
        User expectedUser = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userService.findAll()).thenReturn(Collections.singletonList(expectedUser));
        String expectedJson = "[{\"id\":null,\"roles\":null,\"email\":\"testUser@user\",\"password\":\"test\"}]";

        //When
        mvc.perform(
                get("/userList")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())

                //Then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(expectedJson))
                );
    }

    @Test
    void whenRequestUserList_andNotLogged_thenReturnListOfUsers() throws Exception {

        //Given
        User expectedUser = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userService.findAll()).thenReturn(Collections.singletonList(expectedUser));

        //When
        mvc.perform(
                get("/userList")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())

                //Then
                .andExpect(status().isUnauthorized());
    }
}
