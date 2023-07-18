package com.lukk.sky.message.domain.ports.service;

import com.lukk.sky.message.adapters.dto.MessageDTO;

import java.util.List;

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
    void remove(Long messageId, String username);

    /**
     * Retrieves the messages received by a user.
     *
     * @param userEmail The email of the user.
     * @return The list of messages received by the user.
     */
    List<MessageDTO> getReceivedMessages(String userEmail);

    /**
     * Retrieves the messages sent by a user.
     *
     * @param userEmail The email of the user.
     * @return The list of messages sent by the user.
     */
    List<MessageDTO> getSentMessages(String userEmail);
}
