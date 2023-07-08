package com.lukk.sky.message.domain.ports.service;

import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServicePrimary implements MessageService {

    private final MessageRepository messageRepo;

    @Override
    public MessageDTO send(MessageDTO messageDTO) {

        Message message = messageDTO.toDomain();
        MessageDTO result = MessageDTO.of(messageRepo.save(message));

        log.info("Message sent from " + message.getSenderEmail() + " to " + message.getReceiverEmail());
        return result;
    }

    @Override
    public void remove(Long messageId, String username) {
        messageRepo.findById(messageId).ifPresent(
                (message) -> {
                    if (message.getReceiverEmail().equals(username)
                            || message.getSenderEmail().equals(username)) {
                        messageRepo.delete(message);
                    }
                });
        log.info("Message with ID: {} removed.", messageId);
    }

    @Override
    public List<MessageDTO> getReceivedMessages(String userEmail) {
        List<Message> messages = messageRepo.findAllByReceiverEmail(userEmail);

        return messages.stream()
                .map(MessageDTO::of)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getSentMessages(String userEmail) {
        List<Message> messages = messageRepo.findAllBySenderEmail(userEmail);

        return messages.stream()
                .map(MessageDTO::of)
                .collect(Collectors.toList());
    }


}