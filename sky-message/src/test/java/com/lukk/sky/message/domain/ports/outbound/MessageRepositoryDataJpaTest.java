package com.lukk.sky.message.domain.ports.outbound;

import com.lukk.sky.message.domain.model.Message;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
@Disabled("Re-enable after Testcontainers MySQL replaces H2 and @ConfigurationProperties scanning is wired for @DataJpaTest")
@DisplayName("MessageRepository — JPA slice tests")
class MessageRepositoryDataJpaTest {

    private static final String RECEIVER = "receiver@example.com";
    private static final String SENDER = "sender@example.com";
    private static final String OTHER = "other@example.com";

    @Autowired
    private MessageRepository messageRepository;

    @Test
    @DisplayName("findAllByReceiverEmail returns only messages with the matching receiver")
    void findAllByReceiverEmail_whenReceiverHasMessages_thenReturnOnlyThoseMessages() {
        // given
        save("hello", SENDER, RECEIVER);
        save("hi back", RECEIVER, SENDER);
        save("unrelated", OTHER, SENDER);
        Pageable pageable = PageRequest.of(0, 20);

        // when
        Page<Message> received = messageRepository.findAllByReceiverEmail(RECEIVER, pageable);

        // then
        assertEquals(1, received.getTotalElements());
        assertEquals("hello", received.getContent().get(0).getText());
        assertEquals(SENDER, received.getContent().get(0).getSenderEmail());
    }

    @Test
    @DisplayName("findAllBySenderEmail returns only messages with the matching sender")
    void findAllBySenderEmail_whenSenderHasMessages_thenReturnOnlyThoseMessages() {
        // given
        save("first", SENDER, RECEIVER);
        save("second", SENDER, OTHER);
        save("third", OTHER, RECEIVER);
        Pageable pageable = PageRequest.of(0, 20);

        // when
        Page<Message> sent = messageRepository.findAllBySenderEmail(SENDER, pageable);

        // then
        assertEquals(2, sent.getTotalElements());
        assertTrue(sent.getContent().stream().allMatch(m -> m.getSenderEmail().equals(SENDER)));
    }

    @Test
    @DisplayName("returns empty page when no message matches receiver")
    void findAllByReceiverEmail_whenNoMessagesForReceiver_thenReturnEmptyPage() {
        // given
        save("hello", SENDER, RECEIVER);
        Pageable pageable = PageRequest.of(0, 20);

        // when
        Page<Message> received = messageRepository.findAllByReceiverEmail("nobody@example.com", pageable);

        // then
        assertTrue(received.isEmpty());
    }

    private void save(String text, String sender, String receiver) {
        Message message = Message.builder()
                .text(text)
                .senderEmail(sender)
                .receiverEmail(receiver)
                .createdTime(Instant.now())
                .build();
        messageRepository.save(message);
    }
}
