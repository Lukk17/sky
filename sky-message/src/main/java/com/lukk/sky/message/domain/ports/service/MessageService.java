package com.lukk.sky.message.domain.ports.service;

import com.lukk.sky.message.adapters.dto.MessageDTO;

import java.util.List;

public interface MessageService {
    /**
     * Send message of given text to given receiver.
     *  @param message     Text of message.
     * @param senderEmail Email of message sender
     * @return
     */
    MessageDTO send(MessageDTO messageDTO);

    /**
     * Remove message with given ID.
     *
     * @param messageId ID of message to delete.
     */
    void remove(Long messageId, String username);


    /**
     * Get all messages in which given user is receiver.
     *
     * @param userEmail Receiver mail
     * @return List of Messages as DTO
     */
    List<MessageDTO> getReceivedMessages(String userEmail);


    /**
     * Get all messages in which given user is sender.
     *
     * @param userEmail Sender mail
     * @return List of Messages as DTO
     */
    List<MessageDTO> getSentMessages(String userEmail);
}
