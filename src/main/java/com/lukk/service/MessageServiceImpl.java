package com.lukk.service;

import com.lukk.entity.Message;
import com.lukk.repository.MessageRepository;
import com.lukk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Log4j2
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    final MessageRepository messageRepo;
    final UserRepository userRepo;

    @Override
    public void send(Message message, String senderEmail, Long receiverId) {
        //  entries which users can delete from mailbox:
        message.setReceiver(userRepo.findById(receiverId).orElse(null));
        message.setSender(userRepo.findByEmail(senderEmail));

        //  indelible entries:
        message.setPermaReceiver(message.getReceiver());
        message.setPermaSender(message.getSender());

        message.setRead(false);
        message.setCreated(LocalDateTime.now());
        messageRepo.save(message);

        log.debug("sending message from " + message.getSender() + " to " + message.getReceiver());
    }

    @Override
    public void remove(Long messageId, String userEmail) {

    }

    @Override
    public void read(Long messageId, String receiverEmail) {

    }
}
