package com.lukk.sky.message.domain.ports.inbound;

import com.lukk.sky.message.domain.model.Message;

import java.util.UUID;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;

public record MessageView(
        UUID id,
        String text,
        String receiverEmail,
        String senderEmail,
        String createdTime,
        boolean read) {

    public static MessageView from(Message message) {
        return new MessageView(
                message.getId(),
                message.getText(),
                message.getReceiverEmail(),
                message.getSenderEmail(),
                DATE_TIME_FORMAT.format(message.getCreatedTime()),
                message.isRead());
    }
}
