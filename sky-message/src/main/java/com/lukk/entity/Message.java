package com.lukk.entity;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "Message")
@Table(name = "message")
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Setter(AccessLevel.PROTECTED)
    private Long id;

    @Setter(AccessLevel.PROTECTED)
    private String text;

    private LocalDateTime created;

    private boolean isRead;

    // Without @JoinColumn Hibernate will generate additional table in db for storing id of msg and user
    // which are used for linking this two tables.
//    @ManyToOne
//    @JoinColumn(name = "receiver")
//    @JsonIgnore
//    @ToString.Exclude
//    private User receiver;
//
//    @ManyToOne
//    @JoinColumn(name = "sender")
//    @JsonIgnore
//    @ToString.Exclude
//    private User sender;


}
