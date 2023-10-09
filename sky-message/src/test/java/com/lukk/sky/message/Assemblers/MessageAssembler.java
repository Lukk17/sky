package com.lukk.sky.message.Assemblers;

import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.model.Message;

import java.time.LocalDateTime;
import java.util.List;

import static com.lukk.sky.message.config.Constants.DATE_TIME_FORMAT;

public class MessageAssembler {

    public static final Long TEST_MESSAGE_ID = 1L;
    public static final LocalDateTime CREATED = LocalDateTime.of(2102, 6, 20, 8, 30);
    public static final String RECEIVER_EMAIL = "test@test";
    public static final String SENDER_EMAIL = "sender@test";
    public static final String TEXT = "messageText";
    public static final boolean IS_READ = false;

    public static List<MessageDTO> getMessagesDTO() {
        return List.of(
                getMessageDTO(),
                getMessageDTO());
    }

    public static MessageDTO getMessageDTO() {
        return MessageDTO.builder()
                .createdTime(CREATED.format(DATE_TIME_FORMAT))
                .receiverEmail(RECEIVER_EMAIL)
                .senderEmail(SENDER_EMAIL)
                .text(TEXT)
                .read(IS_READ)
                .build();
    }

    public static List<MessageDTO> getMessagesDTO_withoutCreatedAndID() {
        return List.of(
                getMessageDTO_withoutCreatedAndID(),
                getMessageDTO_withoutCreatedAndID());
    }

    public static MessageDTO getMessageDTO_withoutCreatedAndID() {
        return MessageDTO.builder()
                .receiverEmail(RECEIVER_EMAIL)
                .senderEmail(SENDER_EMAIL)
                .text(TEXT)
                .read(IS_READ)
                .build();
    }

    public static List<Message> getMessages() {
        return List.of(
                getMessage(TEST_MESSAGE_ID),
                getMessage(TEST_MESSAGE_ID + 1));
    }


    public static Message getMessage(Long id) {
        return Message.builder()
                .id(id)
                .createdTime(CREATED)
                .receiverEmail(RECEIVER_EMAIL)
                .senderEmail(SENDER_EMAIL)
                .text(TEXT)
                .isRead(IS_READ)
                .build();
    }
}
