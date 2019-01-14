package com.basic.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity(name = "Message")
@Table (name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String text;

    private LocalDateTime created;

    private boolean readed;

    // Without @JoinColumn Hibernate will generate additional table in db for storing id of msg and user
    // which are used for linking this two tables.

    @ManyToOne
    @JoinColumn(name = "message_receiver")
    private User receiver;

    @ManyToOne
    @JoinColumn(name ="message_sender")
    private User sender;


    //  perma sender and receiver to save who send/receive even if sender/receiver delete it from mailbox

    @ManyToOne
    @JoinColumn(name = "message_permaReceiver")
    private User permaReceiver;

    @ManyToOne
    @JoinColumn(name ="message_permaSender")
    private User permaSender;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public boolean isReaded() {
        return readed;
    }

    public void setReaded(boolean readed) {
        this.readed = readed;
    }

    public User getReceiver() {
        return receiver;
    }

    public void setReceiver(User receiver) {
        this.receiver = receiver;
    }

    public User getSender() {
        return sender;
    }

    public void setSender(User sender) {
        this.sender = sender;
    }

    public User getPermaReceiver() {
        return permaReceiver;
    }

    public void setPermaReceiver(User permaReceiver) {
        this.permaReceiver = permaReceiver;
    }

    public User getPermaSender() {
        return permaSender;
    }

    public void setPermaSender(User permaSender) {
        this.permaSender = permaSender;
    }
}
