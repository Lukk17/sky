package com.basic.service;

import com.basic.entity.Message;
import com.basic.repository.MessageRepository;
import com.basic.repository.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Log4j2
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

        //  indelible entries:
        message.setPermaReceiver(message.getReceiver());
        message.setPermaSender(message.getSender());

        message.setRead(false);
        message.setCreated(LocalDateTime.now());
        messageRepo.save(message);

        log.debug("sending message from "+ message.getSender()+" to "+message.getReceiver());
    }

    @Override
    public void remove(Long messageId, String userEmail) {

    }

    @Override
    public void read(Long messageId, String receiverEmail) {

    }
}
