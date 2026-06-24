package com.lukk.sky.message.domain.ports.service;

import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.exception.MessageException;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * {@inheritDoc}
     * <p>Logs a message when the message is successfully sent.
     */
    @Override
    public MessageDTO send(MessageDTO messageDTO) {
        Message message = messageDTO.toDomain();
        MessageDTO result = MessageDTO.of(messageRepo.save(message));

        log.info("Message sent from {} to {}", message.getSenderEmail(), message.getReceiverEmail());

        return result;
    }

    /**
     * {@inheritDoc}
     * <p>Throws a {@link MessageException} if the message does not exist, or if the user trying to
     * delete the message is neither the sender nor the receiver.
     * Logs a message when the message is successfully deleted.
     */
    @Override
    public void remove(Long messageId, String userId) {
        Message message = messageRepo.findById(messageId)
                .orElseThrow(() -> new MessageException("Message with ID: " + messageId + " not found."));

        if (message.getReceiverEmail().equals(userId)
                || message.getSenderEmail().equals(userId)) {
            messageRepo.delete(message);
            log.info("Message with ID: {} removed.", messageId);
        } else {
            throw new MessageException(String.format(
                    "Can't delete message with ID:%s. User: %s is not receiver nor sender.",
                    messageId, userId));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MessageDTO> getReceivedMessages(String userEmail, Pageable pageable) {
        return messageRepo.findAllByReceiverEmail(userEmail, pageable).map(MessageDTO::of);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<MessageDTO> getSentMessages(String userEmail, Pageable pageable) {
        return messageRepo.findAllBySenderEmail(userEmail, pageable).map(MessageDTO::of);
    }
}
