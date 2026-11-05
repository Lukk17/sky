package com.lukk.sky.message.domain.ports.inbound;

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
     * @param command The command carrying the sender, receiver and text.
     * @return The sent message.
     */
    MessageView send(SendMessageCommand command);

    /**
     * Retrieves a paginated view of the messages received by a user.
     *
     * @param userEmail The email of the user.
     * @param pageable  Pagination and sort parameters.
     * @return A page of messages received by the user.
     */
    Page<MessageView> getReceivedMessages(String userEmail, Pageable pageable);

    /**
     * Retrieves a paginated view of the messages sent by a user.
     *
     * @param userEmail The email of the user.
     * @param pageable  Pagination and sort parameters.
     * @return A page of messages sent by the user.
     */
    Page<MessageView> getSentMessages(String userEmail, Pageable pageable);

    /**
     * Removes a message.
     *
     * @param messageId The ID of the message to be removed.
     * @param userEmail The email of the user making the request.
     * @throws IllegalArgumentException if either {@code messageId} or {@code userEmail} is {@code null}.
     */
    void deleteMessage(UUID messageId, String userEmail);
}
