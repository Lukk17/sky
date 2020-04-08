package com.lukk.sky.message.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank
    // make DB column type "longtext" which can store long strings
    @Type(type = "text")
    private String text;

    private LocalDateTime createdTime;

    private boolean isRead;

    @NotBlank
    private String receiverEmail;

    @NotBlank
    private String senderEmail;


}
