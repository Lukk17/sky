package com.lukk.sky.message.domain.ports.inbound;

import com.lukk.sky.message.adapters.dto.MessageDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for managing messages.
 * This service provides operations for sending, removing, and retrieving messages.
 */
public interface MessageService {

    /**
     * Sends a message.
     *
     * @param messageDTO The message to be sent.
     * @return The sent message.
     */
    MessageDTO send(MessageDTO messageDTO);

    /**
     * Removes a message.
     *
     * @param messageId The ID of the message to be removed.
     * @param username  The username of the user making the request.
     * @throws IllegalArgumentException if either {@code messageId} or {@code username} is {@code null}.
     */
    void remove(UUID messageId, String username);

    /**
     * Retrieves a paginated view of the messages received by a user.
     *
     * @param userEmail The email of the user.
     * @param pageable  Pagination and sort parameters.
     * @return A page of messages received by the user.
     */
    Page<MessageDTO> getReceivedMessages(String userEmail, Pageable pageable);

    /**
     * Retrieves a paginated view of the messages sent by a user.
     *
     * @param userEmail The email of the user.
     * @param pageable  Pagination and sort parameters.
     * @return A page of messages sent by the user.
     */
    Page<MessageDTO> getSentMessages(String userEmail, Pageable pageable);
}
