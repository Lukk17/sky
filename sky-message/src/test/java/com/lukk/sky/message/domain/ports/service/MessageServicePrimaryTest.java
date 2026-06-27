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

import static com.lukk.sky.message.Assemblers.MessageAssembler.RECEIVER_EMAIL;
import static com.lukk.sky.message.Assemblers.MessageAssembler.SENDER_EMAIL;
import static com.lukk.sky.message.Assemblers.MessageAssembler.TEST_MESSAGE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
@DisplayName("MessageServicePrimary — domain service unit tests")
class MessageServicePrimaryTest {

    @Mock
    MessageRepository messageRepository;

    @InjectMocks
    private MessageServicePrimary messageService;

    @Test
    @DisplayName("send() persists the message via repository and returns a DTO matching the saved entity")
    void send_whenValidMessage_thenPersistAndReturnDto() {
        // given
        MessageDTO expected = MessageAssembler.getMessageDTO();
        Message message = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        when(messageRepository.save(any())).thenReturn(message);

        // when
        MessageDTO actual = messageService.send(expected);

        // then
        expected.setId(actual.getId());
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("remove() when caller is the receiver deletes the message from the repository")
    void remove_whenCallerIsReceiver_thenDeleteFromRepository() {
        // given
        Message expected = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        ArgumentCaptor<Message> valueCapture = ArgumentCaptor.forClass(Message.class);
        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(expected));
        doNothing().when(messageRepository).delete(valueCapture.capture());

        // when
        messageService.remove(TEST_MESSAGE_ID, RECEIVER_EMAIL);

        // then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test
    @DisplayName("remove() when caller is neither sender nor receiver throws MessageException")
    void remove_whenCallerIsNeitherSenderNorReceiver_thenThrowMessageException() {
        // given
        Message expected = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(expected));

        // when / then
        assertThrows(MessageException.class, () -> messageService.remove(TEST_MESSAGE_ID, "NOT_EXISTING"));
    }

    @Test
    @DisplayName("remove() when message does not exist throws MessageException")
    void remove_whenMessageDoesNotExist_thenThrowMessageException() {
        // given
        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.empty());

        // when / then
        assertThrows(MessageException.class, () -> messageService.remove(TEST_MESSAGE_ID, RECEIVER_EMAIL));

        verify(messageRepository).findById(TEST_MESSAGE_ID);
    }

    @Test
    @DisplayName("remove() when caller is the sender deletes the message from the repository")
    void remove_whenCallerIsSender_thenDeleteFromRepository() {
        // given
        Message expected = MessageAssembler.getMessage(TEST_MESSAGE_ID);
        ArgumentCaptor<Message> valueCapture = ArgumentCaptor.forClass(Message.class);
        when(messageRepository.findById(TEST_MESSAGE_ID)).thenReturn(Optional.of(expected));
        doNothing().when(messageRepository).delete(valueCapture.capture());

        // when
        messageService.remove(TEST_MESSAGE_ID, SENDER_EMAIL);

        // then
        assertEquals(expected, valueCapture.getValue());
    }

    @Test
    @DisplayName("getReceivedMessages() returns a page of DTOs for all messages addressed to the given user")
    void getReceivedMessages_whenUserHasMessages_thenReturnPagedDtos() {
        // given
        List<MessageDTO> expected = MessageAssembler.getMessagesDTO();
        List<Message> messages = MessageAssembler.getMessages();
        Pageable pageable = PageRequest.of(0, 20);
        when(messageRepository.findAllByReceiverEmail(RECEIVER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(messages, pageable, messages.size()));

        // when
        Page<MessageDTO> actual = messageService.getReceivedMessages(RECEIVER_EMAIL, pageable);

        // then
        assertEquals(2, actual.getTotalElements());
        expected.get(0).setId(actual.getContent().get(0).getId());
        assertEquals(expected.get(0), actual.getContent().get(0));
    }

    @Test
    @DisplayName("getSentMessages() returns a page of DTOs for all messages sent by the given user")
    void getSentMessages_whenUserHasSentMessages_thenReturnPagedDtos() {
        // given
        List<MessageDTO> expected = MessageAssembler.getMessagesDTO();
        List<Message> messages = MessageAssembler.getMessages();
        Pageable pageable = PageRequest.of(0, 20);
        when(messageRepository.findAllBySenderEmail(SENDER_EMAIL, pageable))
                .thenReturn(new PageImpl<>(messages, pageable, messages.size()));

        // when
        Page<MessageDTO> actual = messageService.getSentMessages(SENDER_EMAIL, pageable);

        // then
        assertEquals(2, actual.getTotalElements());
        expected.get(0).setId(actual.getContent().get(0).getId());
        assertEquals(expected.get(0), actual.getContent().get(0));
    }
}
