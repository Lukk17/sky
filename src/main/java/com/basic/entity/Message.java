package com.basic.entity;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity(name = "Message")
@Table (name = "message")
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
    @ManyToOne
    @JoinColumn(name = "receiver")
    private User receiver;

    @ManyToOne
    @JoinColumn(name ="sender")
    private User sender;


    //  perma sender and receiver to save who send/receive even if sender/receiver delete it from mailbox
    @ManyToOne
    @JoinColumn(name = "permaReceiver")
    private User permaReceiver;

    @ManyToOne
    @JoinColumn(name ="permaSender")
    private User permaSender;

}
