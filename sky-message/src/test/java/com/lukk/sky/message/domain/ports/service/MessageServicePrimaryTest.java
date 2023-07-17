package com.lukk.sky.message.domain.ports.service;

import com.lukk.sky.message.Assemblers.MessageAssembler;
import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.exception.MessageException;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static com.lukk.sky.message.Assemblers.MessageAssembler.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class MessageServicePrimaryTest {

    @Mock
    MessageRepository messageRepository;

    @InjectMocks
    private MessageServicePrimary messageService;

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
    public void whenRemoveMessageByReceiver_thenRemoveMessage() {
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
    // This scenario should not happen
    public void whenRemoveMessageOfDifferentUser_thenThrowException() {
        //Given
        Message expected = MessageAssembler.getMessage(TEST_MESSAGE_ID);

        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(expected));

        //Then
        assertThrows(MessageException.class, () -> {

            //When
            messageService.remove(TEST_MESSAGE_ID, "NOT_EXISTING");
        });
    }

    @Test
    public void whenRemoveMessageBySender_thenRemoveMessage() {
        //Given
        Message expected = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        ArgumentCaptor<Message> valueCapture = ArgumentCaptor.forClass(Message.class);

        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(expected));
        doNothing().when(messageRepository).delete(valueCapture.capture());

        //When
        messageService.remove(TEST_MESSAGE_ID, SENDER_EMAIL);

        //Then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test
    public void whenGetReceivedMessages_thenReturnReceivedMessages() {
        //Given
        List<MessageDTO> expected = MessageAssembler.getMessagesDTO();
        List<Message> messages = MessageAssembler.getMessages();

        when(messageRepository.findAllByReceiverEmail(RECEIVER_EMAIL)).thenReturn(messages);

        //When
        List<MessageDTO> actual = messageService.getReceivedMessages(RECEIVER_EMAIL);

        //Then
        assertEquals(expected, actual);
    }

    @Test
    public void whenGetSentMessages_thenReturnSentMessages() {
        //Given
        List<MessageDTO> expected = MessageAssembler.getMessagesDTO();
        List<Message> messages = MessageAssembler.getMessages();

        when(messageRepository.findAllBySenderEmail(SENDER_EMAIL)).thenReturn(messages);

        //When
        List<MessageDTO> actual = messageService.getSentMessages(SENDER_EMAIL);

        //Then
        assertEquals(expected, actual);
    }
}
