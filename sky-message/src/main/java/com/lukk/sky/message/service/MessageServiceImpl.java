package com.lukk.sky.message.service;

import com.lukk.sky.message.dto.EntityDTOConverter;
import com.lukk.sky.message.dto.MessageDTO;
import com.lukk.sky.message.entity.Message;
import com.lukk.sky.message.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepo;

    @Override
    public MessageDTO send(MessageDTO messageDTO) {

        Message message = EntityDTOConverter.convertMessageDTO_toEntity(messageDTO);
        log.info("Sending message from " + message.getSenderEmail() + " to " + message.getReceiverEmail());

        return EntityDTOConverter.convertMessageEntity_toDTO(messageRepo.save(message));
    }

    @Override
    public void remove(Long messageId, String username) {
        messageRepo.findById(messageId).ifPresent(
                (message)->{
                    if(message.getReceiverEmail().equals(username)
                            || message.getSenderEmail().equals(username)){
                        messageRepo.delete(message);
                    }
                });
    }

    @Override
    public List<MessageDTO> getReceivedMessages(String userEmail) {
        List<Message> messages = messageRepo.findAllByReceiverEmail(userEmail);

        return messages.stream()
                .map((EntityDTOConverter::convertMessageEntity_toDTO))
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getSentMessages(String userEmail) {
        List<Message> messages = messageRepo.findAllBySenderEmail(userEmail);

        return messages.stream()
                .map((EntityDTOConverter::convertMessageEntity_toDTO))
                .collect(Collectors.toList());
    }



}
