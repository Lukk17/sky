package com.lukk.service;

import com.lukk.dto.MessageDTO;
import com.lukk.entity.Message;
import com.lukk.entity.User;
import com.lukk.repository.MessageRepository;
import com.lukk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepo;
    private final UserRepository userRepo;

    @Override
    public void send(MessageDTO messageDTO) {

        Message message = convertMessageDTO_toEntity(messageDTO);
        messageRepo.save(message);

        log.info("Sending message from " + message.getSender() + " to " + message.getReceiver());
    }

    @Override
    public void remove(Long messageId) {
        messageRepo.findById(messageId).ifPresent(messageRepo::delete);
    }

    @Override
    public List<MessageDTO> getReceivedMessages(String userEmail) {
        User receiver = userRepo.findByEmail(userEmail);
        List<Message> messages = messageRepo.findAllByReceiver(receiver);

        return messages.stream()
                .map((this::convertMessageEntity_toDTO))
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getSentMessages(String userEmail) {
        User sender = userRepo.findByEmail(userEmail);
        List<Message> messages = messageRepo.findAllBySender(sender);

        return messages.stream()
                .map((this::convertMessageEntity_toDTO))
                .collect(Collectors.toList());
    }

    private Message convertMessageDTO_toEntity(MessageDTO messageDTO) {
        User sender = userRepo.findByEmail(messageDTO.getSender());
        User receiver = userRepo.findByEmail(messageDTO.getReceiver());

        return Message.builder()
                .sender(sender)
                .receiver(receiver)
                .isRead(false)
                .created(LocalDateTime.now())
                .text(messageDTO.getText())
                .build();
    }

    private MessageDTO convertMessageEntity_toDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .created(message.getCreated())
                .isRead(message.isRead())
                .text(message.getText())
                .receiver(message.getReceiver().getEmail())
                .sender(message.getSender().getEmail())
                .build();

    }

}