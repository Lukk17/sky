package com.lukk.sky.message.adapters.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lukk.sky.message.Assemblers.MessageAssembler;
import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.ports.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import org.springframework.context.annotation.Import;

import static com.lukk.sky.message.Assemblers.MessageAssembler.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Import({com.lukk.sky.message.TestSecurityConfig.class, com.lukk.sky.message.TestcontainersConfiguration.class})
@DisplayName("MessageController — HTTP adapter tests")
public class MessageControllerTest {

    private Gson gson;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
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
    @DisplayName("POST /messages with valid payload and JWT returns 2xx and message body")
    public void sendMessage_whenValidMessage_thenReturn2xxWithMessageBody() throws Exception {
//Given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();

        when(messageService.send(any())).thenReturn(messageDTO);

        String expectedJson = gson.toJson(messageDTO);
//When
        MvcResult result = mvc.perform(
                        post("/messages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)))
                                .content(expectedJson)
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andReturn();


        assertTrue(result.getResponse().getContentAsString().contains(messageDTO.getText()));
        assertTrue(result.getResponse().getContentAsString().contains(messageDTO.getReceiverEmail()));
        assertTrue(result.getResponse().getContentAsString().contains(messageDTO.getSenderEmail()));
    }

    @Test
    @DisplayName("POST /messages without JWT returns 401 Unauthorized")
    public void sendMessage_whenNoJwt_thenReturn401() throws Exception {
//Given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();

        when(messageService.send(any())).thenReturn(messageDTO);

        String expectedJson = gson.toJson(messageDTO);
//When
        mvc.perform(
                        post("/messages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(expectedJson)
                )
//Then
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("POST /messages with blank receiverEmail returns 400 and validation error message")
    public void sendMessage_whenReceiverEmailBlank_thenReturnValidationError() throws Exception {
//Given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();
        messageDTO.setReceiverEmail(" ");

        when(messageService.send(any())).thenReturn(messageDTO);

        String expectedJson = gson.toJson(messageDTO);
//When
        MvcResult result = mvc.perform(
                        post("/messages")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)))
                                .content(expectedJson)
                )
//Then
                .andExpect(status().isBadRequest())
                .andReturn();

        assertTrue(result.getResponse().getContentAsString()
                .contains("Field 'receiverEmail' must be a well-formed email address"));
    }

    @Test
    @DisplayName("GET /messages/received with valid JWT returns 2xx and paged received messages")
    public void getReceivedMessages_whenUserHasMessages_thenReturnPagedMessages() throws Exception {
//Given
        List<MessageDTO> messagesDTO = MessageAssembler.getMessagesDTO_withoutCreatedAndID();
        Pageable pageable = PageRequest.of(0, 20);
        when(messageService.getReceivedMessages(eq(RECEIVER_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(messagesDTO, pageable, messagesDTO.size()));
//When
        mvc.perform(
                        get("/messages/received")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", RECEIVER_EMAIL)))
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].receiverEmail").value(RECEIVER_EMAIL))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /messages/received without JWT returns 401 Unauthorized")
    public void getReceivedMessages_whenNoJwt_thenReturn401() throws Exception {
//When
        mvc.perform(
                        get("/messages/received")
                                .contentType(MediaType.APPLICATION_JSON)
                )
//Then
                .andExpect(status().isUnauthorized())
                .andReturn();
    }


    @Test
    @DisplayName("GET /messages/sent with valid JWT returns 2xx and paged sent messages")
    public void getSentMessages_whenUserHasMessages_thenReturnPagedMessages() throws Exception {
//Given
        List<MessageDTO> messagesDTO = MessageAssembler.getMessagesDTO_withoutCreatedAndID();
        Pageable pageable = PageRequest.of(0, 20);
        when(messageService.getSentMessages(eq(SENDER_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(messagesDTO, pageable, messagesDTO.size()));
//When
        mvc.perform(
                        get("/messages/sent")
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)))
                )
//Then
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].senderEmail").value(SENDER_EMAIL))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /messages/sent without JWT returns 401 Unauthorized")
    public void getSentMessages_whenNoJwt_thenReturn401() throws Exception {
//When
        mvc.perform(
                        get("/messages/sent")
                                .contentType(MediaType.APPLICATION_JSON))
//Then
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("DELETE /messages/{id} by receiver returns 2xx")
    public void deleteMessage_whenCalledByReceiver_thenReturn2xx() throws Exception {
//Given
        doNothing().when(messageService).remove(TEST_MESSAGE_ID, RECEIVER_EMAIL);
//When
        mvc.perform(
                        delete(String.format("/messages/%s", TEST_MESSAGE_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", RECEIVER_EMAIL)))
                )
//Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("DELETE /messages/{id} by sender returns 2xx")
    public void deleteMessage_whenCalledBySender_thenReturn2xx() throws Exception {
//Given
        doNothing().when(messageService).remove(TEST_MESSAGE_ID, SENDER_EMAIL);

        String expectedJson = gson.toJson(TEST_MESSAGE_ID);
//When
        mvc.perform(
                        delete(String.format("/messages/%s", TEST_MESSAGE_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)))
                                .content(expectedJson)
                )
//Then
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    @DisplayName("DELETE /messages/{id} without JWT returns 401 Unauthorized")
    public void deleteMessage_whenNoJwt_thenReturn401() throws Exception {
//Given
        doNothing().when(messageService).remove(TEST_MESSAGE_ID, SENDER_EMAIL);

        String expectedJson = gson.toJson(TEST_MESSAGE_ID);
//When
        mvc.perform(
                        delete(String.format("/messages/%s", TEST_MESSAGE_ID))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(expectedJson)
                )
//Then
                .andExpect(status().isUnauthorized());
    }
}
