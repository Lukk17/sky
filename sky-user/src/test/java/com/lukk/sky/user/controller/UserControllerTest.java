package com.lukk.sky.user.controller;

import com.lukk.sky.user.dto.UserDTO;
import com.lukk.sky.user.entity.User;
import com.lukk.sky.user.service.UserService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static com.lukk.sky.user.Assemblers.UserAssembler.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

//    @MockBean
//    private SpringDataUserDetailsServiceImpl springDataUserDetailsService;

    @Test
    void whenPostRegister_thenAddNewUser() throws Exception {
        //Given
        UserDTO testUserDTO = createTestUserDTO(TEST_USER_EMAIL);
        User expectedUser = createTestUser(TEST_USER_EMAIL);
        Mockito.when(userService.saveUser(testUserDTO)).thenReturn(expectedUser);

        //When
        mvc.perform(
                post("/registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJsonUser()))
                .andDo(print())

                //Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
//    @WithMockUser(username = DEFAULT_USER, password = DEFAULT_PASS, roles = "USER")
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
//    @WithMockUser(username = DEFAULT_USER, password = DEFAULT_PASS, roles = "ADMIN")
    void whenRequestUserList_andLogged_thenReturnListOfUsers() throws Exception {

        //Given
        UserDTO expectedUser = createTestUserDTO(TEST_USER_EMAIL);
        Mockito.when(userService.findAllAndConvertToDTO()).thenReturn(Collections.singletonList(expectedUser));
        String expectedJson = "[{\"id\":null,\"email\":\"testUser@user\",\"password\":\"test\",\"roles\":null}]";

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
    void whenRequestUserList_andNotLogged_thenUnauthorized() throws Exception {

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
