package com.lukk.sky.message.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class MessageDTO {

    private Long id;
    private String text;
    private String receiverEmail;
    private String senderEmail;
    private LocalDateTime createdTime;
    private boolean read;
}
