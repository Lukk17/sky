package com.lukk.sky.message.adapters.dto;


import com.lukk.sky.message.domain.model.Message;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

import static com.lukk.sky.common.web.DateTimeConstants.DATE_TIME_FORMAT;
import static com.lukk.sky.common.web.DateTimeConstants.DISPLAY_ZONE;

@Builder
@Data
@AllArgsConstructor
public class MessageDTO {

    private UUID id;

    @Size(max = 65000)
    private String text;

    @NotBlank
    @Email
    private String receiverEmail;

    @Email
    private String senderEmail;

    private String createdTime;
    private Boolean read;

    public static MessageDTO of(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .text(message.getText())
                .receiverEmail(message.getReceiverEmail())
                .senderEmail(message.getSenderEmail())
                .createdTime(DATE_TIME_FORMAT.format(message.getCreatedTime()))
                .read(message.isRead())
                .build();
    }

    public Message toDomain() {
        return Message.builder()
                .text(this.getText())
                .receiverEmail(this.getReceiverEmail())
                .senderEmail(this.getSenderEmail())
                .createdTime(DATE_TIME_FORMAT.parse(this.getCreatedTime(), Instant::from))
                .build();
    }
}
