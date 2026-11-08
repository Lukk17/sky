package com.lukk.sky.message.assemblers;

import com.lukk.sky.contract.dto.MessageDTO;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.inbound.MessageView;
import com.lukk.sky.message.domain.ports.inbound.SendMessageCommand;

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

    public static SendMessageCommand getSendMessageCommand() {
        return new SendMessageCommand(SENDER_EMAIL, RECEIVER_EMAIL, TEXT);
    }

    public static List<MessageView> getMessageViews() {
        return List.of(getMessageView(TEST_MESSAGE_ID), getMessageView(TEST_MESSAGE_ID_2));
    }

    public static MessageView getMessageView(UUID id) {
        return MessageView.from(getMessage(id));
    }

    public static MessageDTO getMessageDTO() {
        MessageDTO messageDTO = getMessageDTO_withoutCreatedAndID();
        messageDTO.setCreatedTime(DATE_TIME_FORMAT.format(CREATED));
        return messageDTO;
    }

    public static List<MessageDTO> getMessagesDTO_withoutCreatedAndID() {
        return List.of(getMessageDTO_withoutCreatedAndID(), getMessageDTO_withoutCreatedAndID());
    }

    public static MessageDTO getMessageDTO_withoutCreatedAndID() {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setReceiverEmail(RECEIVER_EMAIL);
        messageDTO.setSenderEmail(SENDER_EMAIL);
        messageDTO.setText(TEXT);
        messageDTO.setRead(IS_READ);
        return messageDTO;
    }

    public static List<Message> getMessages() {
        return List.of(getMessage(TEST_MESSAGE_ID), getMessage(TEST_MESSAGE_ID_2));
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
