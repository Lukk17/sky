package com.lukk.sky.message.adapters.inbound.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lukk.sky.message.AbstractIntegrationTest;
import com.lukk.sky.message.TestSecurityConfig;
import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.assemblers.MessageAssembler;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.outbound.MessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

import static com.lukk.sky.message.assemblers.MessageAssembler.RECEIVER_EMAIL;
import static com.lukk.sky.message.assemblers.MessageAssembler.SENDER_EMAIL;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Message API: full-stack integration tests")
@Import(TestSecurityConfig.class)
class MessageIntegrationTest extends AbstractIntegrationTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TestPage<T>(List<T> content, long totalElements) {
    }

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    @Test
    @DisplayName("POST /api/v1/messages with valid payload persists and returns 201 with message fields")
    void sendMessage_whenValidPayload_thenPersistAndReturn201() {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO();
        HttpHeaders headers = createTestHttpHeaders(SENDER_EMAIL);
        HttpEntity<MessageDTO> request = new HttpEntity<>(messageDTO, headers);

        // when
        ResponseEntity<MessageDTO> actual = restTemplate.exchange(
                "/api/v1/messages",
                HttpMethod.POST,
                request,
                MessageDTO.class);

        // then
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertMessageFields(messageDTO, requireNonNull(actual.getBody()));
    }

    @Test
    @DisplayName("GET /api/v1/messages/received returns paged messages addressed to the authenticated user")
    void getReceivedMessages_whenUserHasMessages_thenReturnAll() {
        // given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders(RECEIVER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<TestPage<MessageDTO>> actual = restTemplate.exchange(
                "/api/v1/messages/received",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<TestPage<MessageDTO>>() {
                });

        // then
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
    void getSentMessages_whenUserHasMessages_thenReturnAll() {
        // given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders(SENDER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<TestPage<MessageDTO>> actual = restTemplate.exchange(
                "/api/v1/messages/sent",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<TestPage<MessageDTO>>() {
                });

        // then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        List<MessageDTO> content = requireNonNull(actual.getBody()).content();
        assertEquals(2, content.size());
        assertEquals(2, actual.getBody().totalElements());

        for (int i = 0; i < messages.size(); i++) {
            assertMessageFields(MessageDTO.of(messages.get(i)), content.get(i));
        }
    }

    @Test
    @DisplayName("DELETE /api/v1/messages/{id} by receiver removes the message and returns 204 No Content")
    void deleteMessage_whenCalledByReceiver_thenRemoveAndReturn204() {
        // given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders(RECEIVER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<Void> actual = restTemplate.exchange(
                "/api/v1/messages/" + messages.get(0).getId(),
                HttpMethod.DELETE,
                request,
                Void.class);

        // then
        ResponseEntity<TestPage<MessageDTO>> savedMessages = restTemplate.exchange(
                "/api/v1/messages/received",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<TestPage<MessageDTO>>() {
                });

        assertEquals(HttpStatus.NO_CONTENT, actual.getStatusCode());
        assertEquals(1, requireNonNull(savedMessages.getBody()).content().size());
    }

    @Test
    @DisplayName("POST /api/v1/messages with blank text returns 400 with field-errors, never a 500 from the entity constraint")
    void sendMessage_whenTextIsBlank_thenReturn400() {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO();
        messageDTO.setText("   ");
        HttpHeaders headers = createTestHttpHeaders(SENDER_EMAIL);
        HttpEntity<MessageDTO> request = new HttpEntity<>(messageDTO, headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/messages",
                HttpMethod.POST,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
        assertTrue(requireNonNull(actual.getBody()).contains("field-errors"));
        assertTrue(actual.getBody().contains("text"));
        assertEquals(0, messageRepository.count());
    }

    @Test
    @DisplayName("DELETE /api/v1/messages/{id} by a user who is neither sender nor receiver returns 403, not 400")
    void deleteMessage_whenCallerIsNeitherSenderNorReceiver_thenReturn403() {
        // given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders("stranger@test");
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/messages/" + messages.get(0).getId(),
                HttpMethod.DELETE,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.FORBIDDEN, actual.getStatusCode());
        assertEquals(2, messageRepository.count());
    }

    @Test
    @DisplayName("DELETE /api/v1/messages/{id} denial body does not carry the caller's email address")
    void deleteMessage_whenCallerIsNeitherSenderNorReceiver_thenBodyCarriesNoEmailAddress() {
        // given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders("stranger@test");
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/messages/" + messages.get(0).getId(),
                HttpMethod.DELETE,
                request,
                String.class);

        // then
        String body = requireNonNull(actual.getBody());

        assertFalse(body.contains("stranger@test"), "the caller's email is personal data and stays out of the body");
        assertFalse(body.contains(SENDER_EMAIL), "the other parties' emails stay out of the body too");
        assertFalse(body.contains(RECEIVER_EMAIL), "the other parties' emails stay out of the body too");
    }

    @Test
    @DisplayName("GET /api/v1/messages/received with an unknown sort property returns 400 naming it, not a 500")
    void getReceivedMessages_whenSortPropertyIsUnknown_thenReturn400() {
        // given
        HttpHeaders headers = createTestHttpHeaders(RECEIVER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/messages/received?sort=createdTimeee",
                HttpMethod.GET,
                request,
                String.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, actual.getStatusCode());
        assertTrue(requireNonNull(actual.getBody()).contains("createdTimeee"));
    }

    @Test
    @DisplayName("DELETE /api/v1/messages/{id} for an id that does not exist returns 404 naming the id and deletes nothing")
    void deleteMessage_whenMessageDoesNotExist_thenReturn404() {
        // given
        populateDatabaseWithMany();
        UUID unknownId = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        HttpHeaders headers = createTestHttpHeaders(RECEIVER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // when
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/v1/messages/" + unknownId,
                HttpMethod.DELETE,
                request,
                String.class);

        // then
        String body = requireNonNull(actual.getBody());

        assertEquals(HttpStatus.NOT_FOUND, actual.getStatusCode());
        assertTrue(body.contains(unknownId.toString()), "the 404 detail names the id the caller asked for");
        assertEquals(2, messageRepository.count(), "a failed delete leaves both stored messages in place");
    }

    @Test
    @DisplayName("POST /api/v1/messages to an address no identity realm holds returns 201 and persists the message")
    void sendMessage_whenReceiverIsUnknownToAnyRealm_thenPersistAndReturn201() {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO();
        messageDTO.setReceiverEmail("nobody-in-any-realm@test");
        HttpHeaders headers = createTestHttpHeaders(SENDER_EMAIL);
        HttpEntity<MessageDTO> request = new HttpEntity<>(messageDTO, headers);

        // when
        ResponseEntity<MessageDTO> actual = restTemplate.exchange(
                "/api/v1/messages",
                HttpMethod.POST,
                request,
                MessageDTO.class);

        // then
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertEquals("nobody-in-any-realm@test", requireNonNull(actual.getBody()).getReceiverEmail());
        assertEquals(1, messageRepository.count());
    }

    @Test
    @DisplayName("POST /api/v1/messages makes no outbound call, so no Retry-After ever reaches the sender")
    void sendMessage_whenValidPayload_thenResponseCarriesNoRetryAfterHeader() {
        // given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO();
        HttpHeaders headers = createTestHttpHeaders(SENDER_EMAIL);
        HttpEntity<MessageDTO> request = new HttpEntity<>(messageDTO, headers);

        // when
        ResponseEntity<MessageDTO> actual = restTemplate.exchange(
                "/api/v1/messages",
                HttpMethod.POST,
                request,
                MessageDTO.class);

        // then
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertNull(actual.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
    }

    private List<Message> populateDatabaseWithMany() {
        List<Message> messages = MessageAssembler.getMessages();
        messages.forEach(m -> m.setId(null));
        return messageRepository.saveAll(messages);
    }

    private void clearDatabase() {
        messageRepository.deleteAll();
    }

    private static HttpHeaders createTestHttpHeaders(String user) {
        HttpHeaders headers = new HttpHeaders();
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
