package com.lukk.sky.message.Assemblers;

import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.model.Message;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;

public class MessageAssembler {

    public static final UUID TEST_MESSAGE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID TEST_MESSAGE_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final Instant CREATED = Instant.parse("2102-06-20T06:30:00Z");
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
                .createdTime(DATE_TIME_FORMAT.format(CREATED))
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
                getMessage(TEST_MESSAGE_ID_2));
    }


    public static Message getMessage(UUID id) {
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
