package com.lukk.sky.authservice.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.authservice.dto.UserDTO;
import com.lukk.sky.authservice.entity.User;
import com.lukk.sky.authservice.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Collections;

import static com.lukk.sky.authservice.Assemblers.UserAssembler.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class UserControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Before
    public void beforeAll() {
        gson = new GsonBuilder()
                .serializeNulls()
                .create();
    }

    @Test
    public void whenPostRegister_thenAddNewUser() throws Exception {

//Given
        UserDTO testUserDTO = createTestUserDTO_withPassword(TEST_USER_EMAIL);
        User expectedUser = createSimpleTestUser(TEST_USER_EMAIL);
        when(userService.registerUser(testUserDTO)).thenReturn(expectedUser);

//When
        mvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonUser()))
                .andDo(print())

//Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void whenPostRegisterExistingUser_thenThrowBadRequest() throws Exception {

//Given
        String exceptionMessage = "User already Exist!";
        UserDTO testUserDTO = createTestUserDTO_withPassword(TEST_USER_EMAIL);
        when(userService.registerUser(testUserDTO)).thenThrow(new IllegalArgumentException("User already Exist!"));

//When
        MvcResult result = mvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonUser()))
                .andDo(print())

//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        String resultContent = result.getResponse().getContentAsString();

        assertEquals(exceptionMessage, resultContent);

    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, password = TEST_USER_PASSWORD, roles = "ADMIN")
    public void whenRequestUserList_andLoggedAsAdmin_thenReturnListOfUsers() throws Exception {

//Given
        UserDTO expectedUser = createTestUserDTO_withPassword(TEST_USER_EMAIL);
        when(userService.findAllAndConvertToDTO()).thenReturn(Collections.singletonList(expectedUser));

        String expectedJson = gson.toJson(expectedUser);

//When
        MvcResult result = mvc.perform(
                get("/userList")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())

//Then
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString().contains(expectedJson));
    }

    @Test
    public void whenRequestUserList_andNotLogged_thenUnauthorized() throws Exception {

//Given
        UserDTO expectedUser = createTestUserDTO_withPassword(TEST_USER_EMAIL);
        when(userService.findAllAndConvertToDTO()).thenReturn(Collections.singletonList(expectedUser));

//When
        mvc.perform(
                get("/userList")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())

//Then
                .andExpect(status().isUnauthorized()
                );
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, password = TEST_USER_PASSWORD, roles = "USER")
    public void whenRequestUserList_andLoggedAsUser_thenUnauthorized() throws Exception {

//Given
        UserDTO expectedUser = createTestUserDTO_withPassword(TEST_USER_EMAIL);
        when(userService.findAllAndConvertToDTO()).thenReturn(Collections.singletonList(expectedUser));

//When
        mvc.perform(
                get("/userList")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())

//Then
                .andExpect(status().isForbidden()
                );
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, password = TEST_USER_PASSWORD, roles = "USER")
    public void whenRequestUserDetails_andLoggedAsUser_thenReceiveDetails() throws Exception {
//Given
        UserDTO expectedUser = createTestUserDTO_withPassword(TEST_USER_EMAIL);
        String expectedJson = gson.toJson(expectedUser);

        when(userService.findUserDetails(TEST_USER_EMAIL)).thenReturn(expectedUser);

//When
        MvcResult result = mvc.perform(
                get("/userDetails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", TEST_USER_EMAIL))
                .andDo(print())

//Then
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    @WithMockUser(username = TEST_USER_EMAIL, password = TEST_USER_PASSWORD, roles = "USER")
    public void whenRequestUserDetails_andUserNotExist_thenBadRequest() throws Exception {

//Given
        String exceptionMessage = "User already Exist!";

        when(userService.findUserDetails(TEST_USER_EMAIL)).thenThrow(new UsernameNotFoundException(exceptionMessage));

//When
        MvcResult result = mvc.perform(
                get("/userDetails")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", TEST_USER_EMAIL))
                .andDo(print())

//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertEquals(exceptionMessage, result.getResponse().getContentAsString());
    }

}
