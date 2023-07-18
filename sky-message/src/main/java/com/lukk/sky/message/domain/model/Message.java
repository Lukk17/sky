package com.lukk.sky.message.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Message {

    @Id
//    identity will use autoIncrement, AUTO will generate additional table
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
//    lob will make field type as longtext
    @Lob
    @Column(columnDefinition = "TEXT")
    private String text;

    private LocalDateTime createdTime;

    private boolean isRead;

    @NotBlank
    private String receiverEmail;

    @NotBlank
    private String senderEmail;
}
