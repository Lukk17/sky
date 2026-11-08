package com.lukk.sky.message.domain.service;

import com.lukk.sky.message.domain.exception.MessageAccessDeniedException;
import com.lukk.sky.message.domain.exception.MessageNotFoundException;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.inbound.MessageService;
import com.lukk.sky.message.domain.ports.inbound.MessageView;
import com.lukk.sky.message.domain.ports.inbound.SendMessageCommand;
import com.lukk.sky.message.domain.ports.outbound.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Primary implementation of the {@link MessageService}.
 * It uses {@link MessageRepository} to perform operations on the database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@Primary
public class MessageServicePrimary implements MessageService {

    private final MessageRepository messageRepo;
    private final Clock clock;

    @Override
    public MessageView send(SendMessageCommand command) {
        Message message = Message.builder()
                .text(command.text())
                .receiverEmail(command.receiverEmail())
                .senderEmail(command.senderEmail())
                .createdTime(Instant.now(clock))
                .isRead(false)
                .build();
        Message saved = messageRepo.save(message);

        log.info("Message sent from {} to {}", saved.getSenderEmail(), saved.getReceiverEmail());

        return MessageView.from(saved);
    }

    /**
     * {@inheritDoc}
     * <p>Throws a {@link MessageNotFoundException} when the message does not exist, and a
     * {@link MessageAccessDeniedException} when the caller is neither the sender nor the receiver.
     * Logs a message when the message is successfully deleted.
     */
    @Override
    public void deleteMessage(UUID messageId, String userEmail) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new MessageNotFoundException("Message with ID: " + messageId + " not found."));

        if (message.getReceiverEmail().equals(userEmail) || message.getSenderEmail().equals(userEmail)) {
            messageRepo.delete(message);
            log.info("Message with ID: {} removed.", messageId);
        } else {
            throw new MessageAccessDeniedException(
                    "You are neither the sender nor the receiver of this message.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageView> getReceivedMessages(String userEmail, Pageable pageable) {
        return messageRepo.findAllByReceiverEmail(userEmail, pageable).map(MessageView::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MessageView> getSentMessages(String userEmail, Pageable pageable) {
        return messageRepo.findAllBySenderEmail(userEmail, pageable).map(MessageView::from);
    }
}
