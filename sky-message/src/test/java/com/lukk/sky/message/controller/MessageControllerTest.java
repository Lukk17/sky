package com.lukk.sky.message.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.message.Assemblers.MessageAssembler;
import com.lukk.sky.message.dto.MessageDTO;
import com.lukk.sky.message.service.MessageService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static com.lukk.sky.message.Assemblers.MessageAssembler.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class MessageControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MessageService messageService;

    @Before
    public void beforeAll() {
        gson = new GsonBuilder()
                .enableComplexMapKeySerialization()
                .serializeNulls()
                .create();
    }


    @Test
    public void whenGoOnlyDash_thenReturnWelcomingMessage() throws Exception {

//When
        MvcResult result = mvc.perform(
                get("/")
                        .contentType(MediaType.APPLICATION_JSON))

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals("Message service for Sky", result.getResponse().getContentAsString());
    }

    @Test
    public void whenGoHomePage_thenReturnWelcomingMessage() throws Exception {

//When
        MvcResult result = mvc.perform(
                get("/home")
                        .contentType(MediaType.APPLICATION_JSON))

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals("Message service for Sky", result.getResponse().getContentAsString());
    }

    @Test
    public void whenSendMessage_thenReturnMessage() throws Exception {

//Given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();

        when(messageService.send(any())).thenReturn(messageDTO);

        String expectedJson = gson.toJson(messageDTO);

//When
        MvcResult result = mvc.perform(
                post("/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", SENDER_EMAIL)
                        .content(expectedJson)
        )

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenGetReceivedMessages_thenReturnReceivedMessages() throws Exception {

//Given
        List<MessageDTO> messagesDTO = MessageAssembler.getMessagesDTO_withoutCreatedAndID();
        when(messageService.getReceivedMessages(RECEIVER_EMAIL)).thenReturn(messagesDTO);

        String expectedJson = gson.toJson(messagesDTO);

//When
        MvcResult result = mvc.perform(
                get("/received")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", RECEIVER_EMAIL)
        )

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenGetSentMessages_thenReturnSentMessages() throws Exception {

//Given
        List<MessageDTO> messagesDTO = MessageAssembler.getMessagesDTO_withoutCreatedAndID();
        when(messageService.getSentMessages(SENDER_EMAIL)).thenReturn(messagesDTO);

        String expectedJson = gson.toJson(messagesDTO);

//When
        MvcResult result = mvc.perform(
                get("/sent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", SENDER_EMAIL)
        )

//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenReceiverDeleteMessage_thenDeleteAndReturnOk() throws Exception {

//Given
        doNothing().when(messageService).remove(TEST_MESSAGE_ID, RECEIVER_EMAIL);

        String expectedJson = gson.toJson(TEST_MESSAGE_ID);

//When
        mvc.perform(
                delete("/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", RECEIVER_EMAIL)
                        .content(expectedJson)
        )

//Then
                .andExpect(status().is2xxSuccessful());

    }

    @Test
    public void whenSenderDeleteMessage_thenDeleteAndReturnOk() throws Exception {

//Given
        doNothing().when(messageService).remove(TEST_MESSAGE_ID, SENDER_EMAIL);

        String expectedJson = gson.toJson(TEST_MESSAGE_ID);

//When
        mvc.perform(
                delete("/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("username", SENDER_EMAIL)
                        .content(expectedJson)
        )

//Then
                .andExpect(status().is2xxSuccessful());

    }
}
