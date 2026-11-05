package com.lukk.sky.message.adapters.inbound.api;

import com.lukk.sky.contract.dto.MessageDTO;
import com.lukk.sky.message.TestSecurityConfig;
import com.lukk.sky.message.TestcontainersConfiguration;
import com.lukk.sky.message.assemblers.MessageAssembler;
import com.lukk.sky.message.domain.exception.MessageException;
import com.lukk.sky.message.domain.ports.inbound.MessageService;
import com.lukk.sky.message.domain.ports.inbound.MessageView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static com.lukk.sky.message.assemblers.MessageAssembler.RECEIVER_EMAIL;
import static com.lukk.sky.message.assemblers.MessageAssembler.SENDER_EMAIL;
import static com.lukk.sky.message.assemblers.MessageAssembler.TEST_MESSAGE_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestSecurityConfig.class, TestcontainersConfiguration.class})
@DisplayName("MessageController: HTTP adapter tests")
class MessageControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MessageService messageService;

    private final String apiPrefix;

    MessageControllerTest(@Value("${sky.apiPrefix}") String apiPrefix) {
        this.apiPrefix = apiPrefix;
    }

    private MockHttpServletRequestBuilder get(String uri) {
        return MockMvcRequestBuilders.get(apiPrefix + uri);
    }

    private MockHttpServletRequestBuilder post(String uri) {
        return MockMvcRequestBuilders.post(apiPrefix + uri);
    }

    private MockHttpServletRequestBuilder delete(String uri) {
        return MockMvcRequestBuilders.delete(apiPrefix + uri);
    }

    private MessageView storedMessage() {
        return MessageAssembler.getMessageView(TEST_MESSAGE_ID);
    }

    @Test
    @DisplayName("POST /messages with valid payload and JWT returns 201 and message body")
    void sendMessage_whenValidMessage_thenReturn201WithMessageBody() throws Exception {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();
        when(messageService.send(any())).thenReturn(storedMessage());
        String expectedJson = objectMapper.writeValueAsString(messageDTO);

        // when
        MvcResult result = mvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .content(expectedJson))
                .andExpect(status().isCreated())
                .andReturn();

        // then
        assertTrue(result.getResponse().getContentAsString().contains(messageDTO.getText()));
        assertTrue(result.getResponse().getContentAsString().contains(messageDTO.getReceiverEmail()));
        assertTrue(result.getResponse().getContentAsString().contains(SENDER_EMAIL));
    }

    @Test
    @DisplayName("POST /messages without JWT returns 401 Unauthorized")
    void sendMessage_whenNoJwt_thenReturn401() throws Exception {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();
        when(messageService.send(any())).thenReturn(storedMessage());
        String expectedJson = objectMapper.writeValueAsString(messageDTO);

        // when / then
        mvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expectedJson))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("POST /messages with blank receiverEmail returns 400 and validation error message")
    void sendMessage_whenReceiverEmailBlank_thenReturnValidationError() throws Exception {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();
        messageDTO.setReceiverEmail(" ");
        when(messageService.send(any())).thenReturn(storedMessage());
        String expectedJson = objectMapper.writeValueAsString(messageDTO);

        // when
        MvcResult result = mvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .content(expectedJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("receiverEmail"));
        assertTrue(body.contains("field-errors"));
    }

    @Test
    @DisplayName("GET /messages/received with valid JWT returns 200 and paged received messages")
    void getReceivedMessages_whenUserHasMessages_thenReturnPagedMessages() throws Exception {
        // given
        List<MessageView> messages = MessageAssembler.getMessageViews();
        Pageable pageable = PageRequest.of(0, 20);
        when(messageService.getReceivedMessages(eq(RECEIVER_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(messages, pageable, messages.size()));

        // when / then
        mvc.perform(get("/messages/received")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", RECEIVER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].receiverEmail").value(RECEIVER_EMAIL))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /messages/received without JWT returns 401 Unauthorized")
    void getReceivedMessages_whenNoJwt_thenReturn401() throws Exception {
        // when / then
        mvc.perform(get("/messages/received").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("GET /messages/sent with valid JWT returns 200 and paged sent messages")
    void getSentMessages_whenUserHasMessages_thenReturnPagedMessages() throws Exception {
        // given
        List<MessageView> messages = MessageAssembler.getMessageViews();
        Pageable pageable = PageRequest.of(0, 20);
        when(messageService.getSentMessages(eq(SENDER_EMAIL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(messages, pageable, messages.size()));

        // when / then
        mvc.perform(get("/messages/sent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].senderEmail").value(SENDER_EMAIL))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /messages/sent without JWT returns 401 Unauthorized")
    public void getSentMessages_whenNoJwt_thenReturn401() throws Exception {
        // when / then
        mvc.perform(get("/messages/sent").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andReturn();
    }

    @Test
    @DisplayName("DELETE /messages/{id} by receiver returns 204 with no body")
    public void deleteMessage_whenCalledByReceiver_thenReturn204() throws Exception {
        // given
        doNothing().when(messageService).deleteMessage(TEST_MESSAGE_ID, RECEIVER_EMAIL);

        // when / then
        mvc.perform(delete(String.format("/messages/%s", TEST_MESSAGE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", RECEIVER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("DELETE /messages/{id} by sender returns 204 with no body")
    public void deleteMessage_whenCalledBySender_thenReturn204() throws Exception {
        // given
        doNothing().when(messageService).deleteMessage(TEST_MESSAGE_ID, SENDER_EMAIL);

        // when / then
        mvc.perform(delete(String.format("/messages/%s", TEST_MESSAGE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("DELETE /messages/{id} without JWT returns 401 Unauthorized")
    public void deleteMessage_whenNoJwt_thenReturn401() throws Exception {
        // given
        doNothing().when(messageService).deleteMessage(TEST_MESSAGE_ID, SENDER_EMAIL);

        // when / then
        mvc.perform(delete(String.format("/messages/%s", TEST_MESSAGE_ID))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /messages with JWT that has no role returns 403 Forbidden with an empty body")
    void sendMessage_whenJwtHasNoRole_thenReturn403() throws Exception {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();
        when(messageService.send(any())).thenReturn(storedMessage());
        String expectedJson = objectMapper.writeValueAsString(messageDTO);

        // when / then
        mvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)))
                        .content(expectedJson))
                .andExpect(status().isForbidden())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("POST /messages with blank text returns 400 and a field error on text")
    void sendMessage_whenTextIsBlank_thenReturnValidationError() throws Exception {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();
        messageDTO.setText("  ");
        when(messageService.send(any())).thenReturn(storedMessage());
        String payload = objectMapper.writeValueAsString(messageDTO);

        // when
        MvcResult result = mvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andReturn();

        // then
        String body = result.getResponse().getContentAsString();

        assertTrue(body.contains("field-errors"));
        assertTrue(body.contains("text"));
    }

    @Test
    @DisplayName("POST /messages when the domain rejects the send returns 400 with the domain message as the problem detail")
    void sendMessage_whenDomainThrowsMessageException_thenReturn400WithDomainMessage() throws Exception {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();
        when(messageService.send(any())).thenThrow(new MessageException("Message could not be accepted."));
        String payload = objectMapper.writeValueAsString(messageDTO);

        // when / then
        mvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid request."));
    }

    @Test
    @DisplayName("POST /messages with a senderEmail that is not an email still succeeds, because the server assigns it")
    void sendMessage_whenSenderEmailIsGarbage_thenIgnoreItAndSucceed() throws Exception {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO_withoutCreatedAndID();
        messageDTO.setSenderEmail("not-an-email");
        when(messageService.send(any())).thenReturn(storedMessage());
        String payload = objectMapper.writeValueAsString(messageDTO);

        // when / then
        mvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(jwt().jwt(j -> j.claim("email", SENDER_EMAIL)).authorities(new SimpleGrantedAuthority("ROLE_USER")))
                        .content(payload))
                .andExpect(status().isCreated());
    }
}
