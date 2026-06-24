package com.lukk.sky.message.domain.ports.service;

import com.lukk.sky.message.Assemblers.MessageAssembler;
import com.lukk.sky.message.adapters.dto.MessageDTO;
import com.lukk.sky.message.domain.exception.MessageException;
import com.lukk.sky.message.domain.model.Message;
import com.lukk.sky.message.domain.ports.repository.MessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static com.lukk.sky.message.Assemblers.MessageAssembler.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("MessageServicePrimary — domain service unit tests")
public class MessageServicePrimaryTest {

    @Mock
    MessageRepository messageRepository;

    @InjectMocks
    private MessageServicePrimary messageService;

    @Test
    @DisplayName("send() persists the message via repository and returns a DTO matching the saved entity")
    public void send_whenValidMessage_thenPersistAndReturnDto() {
        //Given
        MessageDTO expected = MessageAssembler.getMessageDTO();
        Message message = MessageAssembler.getMessage(TEST_MESSAGE_ID);

        // created time is different so any() need to be used
        when(messageRepository.save(any())).thenReturn(message);

        //When
        MessageDTO actual = messageService.send(expected);

        //Then
        // for comparison
        expected.setId(actual.getId());
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("remove() when caller is the receiver deletes the message from the repository")
    public void remove_whenCallerIsReceiver_thenDeleteFromRepository() {
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
    @DisplayName("remove() when caller is neither sender nor receiver throws MessageException")
    public void remove_whenCallerIsNeitherSenderNorReceiver_thenThrowMessageException() {
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
    @DisplayName("remove() when message does not exist throws MessageException")
    public void remove_whenMessageDoesNotExist_thenThrowMessageException() {
        //Given
        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.empty());

        //Then
        assertThrows(MessageException.class, () -> {

            //When
            messageService.remove(TEST_MESSAGE_ID, RECEIVER_EMAIL);
        });

        verify(messageRepository).findById(TEST_MESSAGE_ID);
    }

    @Test
    @DisplayName("remove() when caller is the sender deletes the message from the repository")
    public void remove_whenCallerIsSender_thenDeleteFromRepository() {
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
    @DisplayName("getReceivedMessages() returns a page of DTOs for all messages addressed to the given user")
    public void getReceivedMessages_whenUserHasMessages_thenReturnPagedDtos() {
        //Given
        List<MessageDTO> expected = MessageAssembler.getMessagesDTO();
        List<Message> messages = MessageAssembler.getMessages();
        Pageable pageable = PageRequest.of(0, 20);

        when(messageRepository.findAllByReceiverEmail(RECEIVER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(messages, pageable, messages.size()));

        //When
        Page<MessageDTO> actual = messageService.getReceivedMessages(RECEIVER_EMAIL, pageable);

        //Then
        assertEquals(2, actual.getTotalElements());
        // for comparison
        expected.get(0).setId(actual.getContent().get(0).getId());
        assertEquals(expected.get(0), actual.getContent().get(0));
    }

    @Test
    @DisplayName("getSentMessages() returns a page of DTOs for all messages sent by the given user")
    public void getSentMessages_whenUserHasSentMessages_thenReturnPagedDtos() {
        //Given
        List<MessageDTO> expected = MessageAssembler.getMessagesDTO();
        List<Message> messages = MessageAssembler.getMessages();
        Pageable pageable = PageRequest.of(0, 20);

        when(messageRepository.findAllBySenderEmail(SENDER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(messages, pageable, messages.size()));

        //When
        Page<MessageDTO> actual = messageService.getSentMessages(SENDER_EMAIL, pageable);

        //Then
        assertEquals(2, actual.getTotalElements());
        // for comparison
        expected.get(0).setId(actual.getContent().get(0).getId());
        assertEquals(expected.get(0), actual.getContent().get(0));
    }
}
