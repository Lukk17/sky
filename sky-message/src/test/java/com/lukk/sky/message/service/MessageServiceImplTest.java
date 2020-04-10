package com.lukk.sky.message.service;

import com.lukk.sky.message.Assemblers.MessageAssembler;
import com.lukk.sky.message.H2TestProfileJPAConfig;
import com.lukk.sky.message.SkyMessageApplication;
import com.lukk.sky.message.dto.MessageDTO;
import com.lukk.sky.message.entity.Message;
import com.lukk.sky.message.repository.MessageRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;
import java.util.Optional;

import static com.lukk.sky.message.Assemblers.MessageAssembler.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
        SkyMessageApplication.class,
        H2TestProfileJPAConfig.class})
@ActiveProfiles("test")
public class MessageServiceImplTest {

    @Autowired
    private MessageService messageService;

    @MockBean
    MessageRepository messageRepository;

    @Test
    public void whenSendMessage_thenSaveAndReturnMessage() {
        //Given
        MessageDTO expected = MessageAssembler.getMessageDTO(TEST_MESSAGE_ID);
        Message message = MessageAssembler.getMessage(TEST_MESSAGE_ID);

        // created time is different so any() need to be used
        when(messageRepository.save(any())).thenReturn(message);

        //When
        MessageDTO actual = messageService.send(expected);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenRemoveMessage_thenRemoveMessage() {
        //Given
        Message expected = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        ArgumentCaptor<Message> valueCapture = ArgumentCaptor.forClass(Message.class);

        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(expected));
        doNothing().when(messageRepository).delete(valueCapture.capture());

        //When
        messageService.remove(TEST_MESSAGE_ID, RECEIVER_EMAIL);

        //Then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test
    public void whenGetReceivedMessages_thenReturnReceivedMessages() {
        //Given
        List<MessageDTO> expected =  MessageAssembler.getMessagesDTO();
        List<Message> messages =  MessageAssembler.getMessages();

        when(messageRepository.findAllByReceiverEmail(RECEIVER_EMAIL)).thenReturn(messages);

        //When
        List<MessageDTO> actual =messageService.getReceivedMessages(RECEIVER_EMAIL);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenGetSentMessages_thenReturnSentMessages() {
        //Given
        List<MessageDTO> expected =  MessageAssembler.getMessagesDTO();
        List<Message> messages =  MessageAssembler.getMessages();

        when(messageRepository.findAllBySenderEmail(SENDER_EMAIL)).thenReturn(messages);

        //When
        List<MessageDTO> actual =messageService.getSentMessages(SENDER_EMAIL);

        //Then
        assertEquals(expected, actual);
    }
}
