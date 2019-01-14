package com.basic.service;

import com.basic.entity.Message;
import com.basic.repository.MessageRepository;
import com.basic.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class MessageService implements IMessageService
{
    @Autowired
    MessageRepository messageRepo;

    @Autowired
    UserRepository userRepo;

    @Override
    public void send(Message message, String senderEmail, Long receiverId)
    {
        //  entries which users can delete from mailbox:
        message.setReceiver(userRepo.findById(receiverId).get());
        message.setSender(userRepo.findByEmail(senderEmail));

        //  undeletable entries:
        message.setPermaReceiver(message.getReceiver());
        message.setPermaSender(message.getSender());

        message.setReaded(false);
        message.setCreated(LocalDateTime.now());
        messageRepo.save(message);
    }

    @Override
    public void remove(Long messageId, String userEmail) {

    }

    @Override
    public void readed(Long messageId, String receiverEmail) {

    }
}
