package com.basic.service;

import com.basic.entity.Message;

public interface IMessageService
{
    /**
     * Send message of given text to given receiver.
     *
     * @param message           Text of message.
     * @param senderEmail       Mail of sender.
     * @param receiverId        ID of message receiver.
     */
    void send(Message message, String senderEmail, Long receiverId);

    /**
     * Remove message with given ID.
     * User email confirm authorization for deleting.
     * Message will be still visible to sender/receiver.
     *
     * @param messageId     ID of message to delete.
     * @param userEmail     Email of user who delete message.
     */
    void remove(Long messageId, String userEmail);

    /**
     * Set read attribute of given Message to true,
     * if given user is receiver.
     *
     * @param messageId         ID of Message.
     * @param receiverEmail     Email of message receiver.
     */
    void read(Long messageId, String receiverEmail);
}
