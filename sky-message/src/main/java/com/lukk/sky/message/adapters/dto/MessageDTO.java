package com.lukk.sky.message.adapters.dto;


import com.lukk.sky.message.domain.model.Message;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import static com.lukk.sky.message.config.Constants.DATE_TIME_FORMAT;

@Builder
@Data
public class MessageDTO {

    private Long id;

    @Size(max = 65000)
    private String text;

    @NotBlank
    @Email
    private String receiverEmail;

    @Email
    private String senderEmail;

    private String createdTime;
    private boolean read;

    public static MessageDTO of(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .text(message.getText())
                .receiverEmail(message.getReceiverEmail())
                .senderEmail(message.getSenderEmail())
                .createdTime(message.getCreatedTime().format(DATE_TIME_FORMAT))
                .build();
    }

    public Message toDomain() {
        return Message.builder()
                .text(this.getText())
                .receiverEmail(this.getReceiverEmail())
                .senderEmail(this.getSenderEmail())
                .createdTime(LocalDateTime.parse(this.getCreatedTime(), DATE_TIME_FORMAT))
                .build();
    }
}
