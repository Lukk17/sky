package com.lukk.sky.message.adapters.api;

import com.lukk.sky.message.Assemblers.MessageAssembler;
import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.repository.MessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static com.lukk.sky.message.Assemblers.MessageAssembler.RECEIVER_EMAIL;
import static com.lukk.sky.message.Assemblers.MessageAssembler.SENDER_EMAIL;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class MessageIntegrationTest {

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
    public void testCreateMessage() {
        // Given
        MessageDTO messageDTO = MessageAssembler.getMessageDTO();

        HttpHeaders headers = createTestHttpHeaders(SENDER_EMAIL);
        HttpEntity<MessageDTO> request = new HttpEntity<>(messageDTO, headers);

        // When
        ResponseEntity<MessageDTO> actual = restTemplate.exchange(
                "/api/message",
                HttpMethod.POST,
                request,
                MessageDTO.class);

        // Then
        assertEquals(HttpStatus.CREATED, actual.getStatusCode());
        assertMessageFields(messageDTO, requireNonNull(actual.getBody()));
    }

    @Test
    public void testGetAllReceivedMessages() {
        // Given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders(RECEIVER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<MessageDTO[]> actual = restTemplate.exchange(
                "/api/messages/received",
                HttpMethod.GET,
                request,
                MessageDTO[].class);

        // Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(2, requireNonNull(actual.getBody()).length);

        for (int i = 0; i < messages.size(); i++) {
            assertMessageFields(MessageDTO.of(messages.get(i)), requireNonNull(actual.getBody())[i]);
        }
    }

    @Test
    public void testGetAllSentMessages() {
        // Given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders(SENDER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<MessageDTO[]> actual = restTemplate.exchange(
                "/api/messages/sent",
                HttpMethod.GET,
                request,
                MessageDTO[].class);

        // Then
        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals(2, requireNonNull(actual.getBody()).length);

        for (int i = 0; i < messages.size(); i++) {
            assertMessageFields(MessageDTO.of(messages.get(i)), requireNonNull(actual.getBody())[i]);
        }
    }

    @Test
    public void testDeleteMessage() {
        // Given
        List<Message> messages = populateDatabaseWithMany();
        HttpHeaders headers = createTestHttpHeaders(RECEIVER_EMAIL);
        HttpEntity<?> request = new HttpEntity<>(headers);

        // When
        ResponseEntity<String> actual = restTemplate.exchange(
                "/api/message/" + messages.get(0).getId(),
                HttpMethod.DELETE,
                request,
                String.class);
        //
        // Then
        ResponseEntity<MessageDTO[]> savedMessages = restTemplate.exchange(
                "/api/messages/received",
                HttpMethod.GET,
                request,
                MessageDTO[].class);

        assertEquals(HttpStatus.OK, actual.getStatusCode());
        assertEquals("Message removed.", actual.getBody());
        assertEquals(1, requireNonNull(savedMessages.getBody()).length);
    }

    private List<Message> populateDatabaseWithMany() {
        return messageRepository.saveAll(MessageAssembler.getMessages());
    }

    private void clearDatabase() {
        messageRepository.deleteAll();
    }

    private static HttpHeaders createTestHttpHeaders(String user) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Forwarded-User", user);
        return headers;
    }

    private static void assertMessageFields(MessageDTO expected, MessageDTO actual) {
        assertEquals(expected.getText(), actual.getText());
        assertEquals(expected.getSenderEmail(), actual.getSenderEmail());
        assertEquals(expected.getReceiverEmail(), actual.getReceiverEmail());
    }
}
