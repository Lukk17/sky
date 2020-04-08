package com.lukk.sky.authservice.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class HomeControllerTest {

    @Autowired
    private MockMvc mvc;


    @Test
    public void whenRequestStartPage_thenReturnString() throws Exception {

        //When
        mvc.perform(
                get("/"))
                .andDo(print())

                //Then
                .andExpect(status().isOk());
    }

    @Test
    public void whenRequestHomePage_thenReturnString() throws Exception {

        //When
        mvc.perform(
                get("/home"))
                .andDo(print())

                //Then
                .andExpect(status().isOk());
    }
}
