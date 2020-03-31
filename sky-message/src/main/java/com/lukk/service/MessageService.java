package com.lukk.service;

import com.lukk.dto.MessageDTO;

public interface MessageService {
    /**
     * Send message of given text to given receiver.
     *
     * @param message     Text of message.
     * @param senderEmail Email of message sender
     */
    void send(MessageDTO messageDTO);

    /**
     * Remove message with given ID.
     *
     * @param messageId ID of message to delete.
     */
    void remove(Long messageId);


    /**
     * Get all messages in which given user is receiver.
     *
     * @param userEmail Receiver mail
     * @return List of Messages as DTO
     */
//    List<MessageDTO> getReceivedMessages(String userEmail);


    /**
     * Get all messages in which given user is sender.
     *
     * @param userEmail Sender mail
     * @return List of Messages as DTO
     */
//    List<MessageDTO> getSentMessages(String userEmail);
}
