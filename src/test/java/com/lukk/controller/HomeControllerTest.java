package com.lukk.controller;

import com.lukk.service.SpringDataUserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SpringDataUserDetailsServiceImpl springDataUserDetailsService;

    @Test
    void home() throws Exception {

        //When
        mvc.perform(
                get("/"))
                .andDo(print())

                //Then
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hello World!"))
                );
    }
}
