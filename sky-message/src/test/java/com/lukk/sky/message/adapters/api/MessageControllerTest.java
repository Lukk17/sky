package com.lukk.sky.message.adapters.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.message.Assemblers.MessageAssembler;
import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.ports.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static com.lukk.sky.message.Assemblers.MessageAssembler.*;
import static com.lukk.sky.message.config.Constants.USER_INFO_HEADERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class MessageControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MessageService messageService;

    private final String API_PREFIX;

    public MessageControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
        this.API_PREFIX = apiPrefix;
    }

    private MockHttpServletRequestBuilder get(String uri) {
        return MockMvcRequestBuilders.get("/" + API_PREFIX + uri);
    }

    private MockHttpServletRequestBuilder post(String uri) {
        return MockMvcRequestBuilders.post("/" + API_PREFIX + uri);
    }

    private MockHttpServletRequestBuilder delete(String uri) {
        return MockMvcRequestBuilders.delete("/" + API_PREFIX + uri);
    }

    @BeforeEach
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

        assertEquals("<center><h1>Welcome to Message app.</h1></center>", result.getResponse().getContentAsString());
    }

    @Test
    public void whenGoHomePage_thenReturnWelcomingMessage() throws Exception {
//When
        MvcResult result = mvc.perform(
                        get("/home")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(USER_INFO_HEADERS.iterator().next(), RECEIVER_EMAIL))
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals("<center><h1>Welcome to Message app.</h1></center>", result.getResponse().getContentAsString());
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
                                .header(USER_INFO_HEADERS.iterator().next(), RECEIVER_EMAIL)
                                .content(expectedJson)
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenSendMessageWithoutUser_thenReturnError() throws Exception {
//Given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();

        when(messageService.send(any())).thenReturn(messageDTO);

        String expectedJson = gson.toJson(messageDTO);
//When
        mvc.perform(
                        post("/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(expectedJson)
                )
//Then
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void whenSendMessage_AndValidationError_thenReturnError() throws Exception {
//Given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();
        messageDTO.setReceiverEmail(" ");

        when(messageService.send(any())).thenReturn(messageDTO);

        String expectedJson = gson.toJson(messageDTO);
//When
        MvcResult result = mvc.perform(
                        post("/send")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(expectedJson)
                )
//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString()
                .contains("Field 'receiverEmail' must be a well-formed email address"));
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
                                .header(USER_INFO_HEADERS.iterator().next(), RECEIVER_EMAIL)
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenGetReceivedMessagesWithoutUser_thenReturnError() throws Exception {
//Given
        List<MessageDTO> messagesDTO = MessageAssembler.getMessagesDTO_withoutCreatedAndID();
        when(messageService.getReceivedMessages(RECEIVER_EMAIL)).thenReturn(messagesDTO);
//When
        mvc.perform(
                        get("/received")
                                .contentType(MediaType.APPLICATION_JSON)
                )
//Then
                .andExpect(status().isBadRequest())
                .andReturn();
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
                                .header(USER_INFO_HEADERS.iterator().next(), SENDER_EMAIL)
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        assertEquals(expectedJson, result.getResponse().getContentAsString());
    }

    @Test
    public void whenGetSentMessagesWithoutUser_thenReturnError() throws Exception {
//Given
        List<MessageDTO> messagesDTO = MessageAssembler.getMessagesDTO_withoutCreatedAndID();
        when(messageService.getSentMessages(SENDER_EMAIL)).thenReturn(messagesDTO);
//When
        mvc.perform(
                        get("/sent")
                                .contentType(MediaType.APPLICATION_JSON))
//Then
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void whenReceiverDeleteMessage_thenDeleteAndReturnOk() throws Exception {
//Given
        doNothing().when(messageService).remove(TEST_MESSAGE_ID, RECEIVER_EMAIL);
//When
        mvc.perform(
                        delete(String.format("/delete/%s", TEST_MESSAGE_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(USER_INFO_HEADERS.iterator().next(), RECEIVER_EMAIL)
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
                        delete(String.format("/delete/%s", TEST_MESSAGE_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(USER_INFO_HEADERS.iterator().next(), SENDER_EMAIL)
                                .content(expectedJson)
                )
//Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void whenDeleteWithoutUserInfo_thenReturnBadRequest() throws Exception {
//Given
        doNothing().when(messageService).remove(TEST_MESSAGE_ID, SENDER_EMAIL);

        String expectedJson = gson.toJson(TEST_MESSAGE_ID);
//When
        mvc.perform(
                        delete(String.format("/delete/%s", TEST_MESSAGE_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(expectedJson)
                )
//Then
                .andExpect(status().isBadRequest());
    }
}
