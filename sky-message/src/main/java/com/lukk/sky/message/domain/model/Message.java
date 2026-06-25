package com.lukk.sky.message.domain.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "senderEmail", "receiverEmail"})
@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String text;

    private LocalDateTime createdTime;

    private boolean isRead;

    @NotBlank
    @Email
    private String receiverEmail;

    @NotBlank
    @Email
    private String senderEmail;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }

        Message other = (Message) o;

        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
