package com.lukk.sky.message.adapters.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lukk.sky.message.AbstractIntegrationTest;
import com.lukk.sky.message.Assemblers.MessageAssembler;
import com.lukk.sky.message.TestSecurityConfig;
import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.repository.MessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;

import static com.lukk.sky.message.Assemblers.MessageAssembler.RECEIVER_EMAIL;
import static com.lukk.sky.message.Assemblers.MessageAssembler.SENDER_EMAIL;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Message API — full-stack integration tests")
@Import(TestSecurityConfig.class)
public class MessageIntegrationTest extends AbstractIntegrationTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TestPage<T>(List<T> content, long totalElements) {}

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
    }

    @AfterEach
    public void tearDown() {
        clearDatabase();
    }

    @Test
    @DisplayName("POST /api/v1/messages with valid payload persists and returns 201 with message fields")
    public void sendMessage_whenValidPayload_thenPersistAndReturn201() {
        // Given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO();

        HttpHeaders headers = createTestHttpHeaders(SENDER_EMAIL);
        HttpEntity<MessageDTO> request = new HttpEntity<>(messageDTO, headers);

        // When
        ResponseEntity<MessageDTO> actual = restTemplate.exchange(
                "/api/v1/messages",
                HttpMethod.POST,
                request,
                MessageDTO.class);

        // Then
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertMessageFields(messageDTO, requireNonNull(actual.getBody()));
    }

    @Test
    @DisplayName("GET /api/v1/messages/received returns paged messages addressed to the authenticated user")
    public void getReceivedMessages_whenUserHasMessages_thenReturnAll() {
        // Given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders(RECEIVER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<TestPage<MessageDTO>> actual = restTemplate.exchange(
                "/api/v1/messages/received",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<TestPage<MessageDTO>>() {});

        // Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        List<MessageDTO> content = requireNonNull(actual.getBody()).content();
        assertEquals(2, content.size());
        assertEquals(2, actual.getBody().totalElements());

        for (int i = 0; i < messages.size(); i++) {
            assertMessageFields(MessageDTO.of(messages.get(i)), content.get(i));
        }
    }

    @Test
    @DisplayName("GET /api/v1/messages/sent returns paged messages sent by the authenticated user")
    public void getSentMessages_whenUserHasMessages_thenReturnAll() {
        // Given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders(SENDER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<TestPage<MessageDTO>> actual = restTemplate.exchange(
                "/api/v1/messages/sent",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<TestPage<MessageDTO>>() {});

        // Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        List<MessageDTO> content = requireNonNull(actual.getBody()).content();
        assertEquals(2, content.size());
        assertEquals(2, actual.getBody().totalElements());

        for (int i = 0; i < messages.size(); i++) {
            assertMessageFields(MessageDTO.of(messages.get(i)), content.get(i));
        }
    }

    @Test
    @DisplayName("DELETE /api/v1/messages/{id} by receiver removes the message and returns 200 with confirmation")
    public void deleteMessage_whenCalledByReceiver_thenRemoveAndReturn200() {
        // Given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders(RECEIVER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/messages/" + messages.get(0).getId(),
                HttpMethod.DELETE,
                request,
                String.class);
        //
        // Then
        ResponseEntity<TestPage<MessageDTO>> savedMessages = restTemplate.exchange(
                "/api/v1/messages/received",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<TestPage<MessageDTO>>() {});

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals("Message removed.", actual.getBody());
        assertEquals(1, requireNonNull(savedMessages.getBody()).content().size());
    }

    private List<Message> populateDatabaseWithMany() {
        // Null the assembler-hardcoded ids so saveAll() takes the INSERT path on
        // IDENTITY columns rather than treating fixed ids as detached entities.
        List<Message> messages = MessageAssembler.getMessages();
        messages.forEach(m -> m.setId(null));
        return messageRepository.saveAll(messages);
    }

    private void clearDatabase() {
        messageRepository.deleteAll();
    }

    private static HttpHeaders createTestHttpHeaders(String user) {
        HttpHeaders headers = new HttpHeaders();
        // base64url so the stub JwtDecoder (TestSecurityConfig) decodes it back to the email claim;
        // a raw email contains '@', which is outside the RFC 6750 Bearer-token charset.
        headers.setBearerAuth(java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(user.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return headers;
    }

    private static void assertMessageFields(MessageDTO expected, MessageDTO actual) {
        assertEquals(expected.getText(), actual.getText());
        assertEquals(expected.getSenderEmail(), actual.getSenderEmail());
        assertEquals(expected.getReceiverEmail(), actual.getReceiverEmail());
    }
}
