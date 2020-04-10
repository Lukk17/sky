package com.lukk.sky.message.dto;

import com.lukk.sky.message.entity.Message;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EntityDTOConverter {

    public static Message convertMessageDTO_toEntity(MessageDTO messageDTO) {

        return Message.builder()
                .senderEmail(messageDTO.getSenderEmail())
                .receiverEmail(messageDTO.getReceiverEmail())
                .isRead(false)
                .createdTime(LocalDateTime.now())
                .text(messageDTO.getText())
                .build();
    }

    public static MessageDTO convertMessageEntity_toDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .createdTime(message.getCreatedTime())
                .read(message.isRead())
                .text(message.getText())
                .receiverEmail(message.getReceiverEmail())
                .senderEmail(message.getSenderEmail())
                .build();

    }

}
