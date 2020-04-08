package com.lukk.sky.message.service;

import com.lukk.sky.message.dto.MessageDTO;
import com.lukk.sky.message.entity.Message;
import com.lukk.sky.message.repository.MessageRepository;
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

    @Override
    public void send(MessageDTO messageDTO) {

        Message message = convertMessageDTO_toEntity(messageDTO);
        messageRepo.save(message);

        log.info("Sending message from " + message.getSenderEmail() + " to " + message.getReceiverEmail());
    }

    @Override
    public void remove(Long messageId) {
        messageRepo.findById(messageId).ifPresent(messageRepo::delete);
    }

    @Override
    public List<MessageDTO> getReceivedMessages(String userEmail) {
        List<Message> messages = messageRepo.findAllByReceiverEmail(userEmail);

        return messages.stream()
                .map((this::convertMessageEntity_toDTO))
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getSentMessages(String userEmail) {
        List<Message> messages = messageRepo.findAllBySenderEmail(userEmail);

        return messages.stream()
                .map((this::convertMessageEntity_toDTO))
                .collect(Collectors.toList());
    }

    private Message convertMessageDTO_toEntity(MessageDTO messageDTO) {

        return Message.builder()
                .senderEmail(messageDTO.getSenderEmail())
                .receiverEmail(messageDTO.getReceiverEmail())
                .isRead(false)
                .createdTime(LocalDateTime.now())
                .text(messageDTO.getText())
                .build();
    }

    private MessageDTO convertMessageEntity_toDTO(Message message) {
        return MessageDTO.builder()
                .id(message.getId())
                .createdTime(message.getCreatedTime())
                .isRead(message.isRead())
                .text(message.getText())
                .receiverEmail(message.getReceiverEmail())
                .senderEmail(message.getSenderEmail())
                .build();

    }

}
