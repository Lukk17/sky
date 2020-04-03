package com.lukk.sky.message.dto;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class MessageDTO {

    private Long id;
    private String text;
    private String receiver;
    private String sender;
    private LocalDateTime created;
    private boolean isRead;
}
